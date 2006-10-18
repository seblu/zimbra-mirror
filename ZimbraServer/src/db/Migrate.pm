# 
# ***** BEGIN LICENSE BLOCK *****
# Version: MPL 1.1
# 
# The contents of this file are subject to the Mozilla Public License
# Version 1.1 ("License"); you may not use this file except in
# compliance with the License. You may obtain a copy of the License at
# http://www.zimbra.com/license
# 
# Software distributed under the License is distributed on an "AS IS"
# basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
# the License for the specific language governing rights and limitations
# under the License.
# 
# The Original Code is: Zimbra Collaboration Suite Server.
# 
# The Initial Developer of the Original Code is Zimbra, Inc.
# Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
# All Rights Reserved.
# 
# Contributor(s):
# 
# ***** END LICENSE BLOCK *****
# 
package Migrate;

use strict;
use DBI;
use POSIX qw(:signal_h :errno_h :sys_wait_h);

#############

my $MYSQL = "mysql";
my $LOGMYSQL = "/opt/zimbra/bin/logmysql";
my $DB_USER = "zimbra";
my $DB_PASSWORD = "zimbra";
my $LOGGER_DB_PASSWORD = "zimbra";
my $DATABASE = "zimbra";
my $LOGGER_DATABASE = "zimbra_logger";
my $ZIMBRA_HOME = $ENV{ZIMBRA_HOME} || '/opt/zimbra';
my $ZMLOCALCONFIG = "$ZIMBRA_HOME/bin/zmlocalconfig";

if ($^O !~ /MSWin/i) {
    $DB_PASSWORD = `$ZMLOCALCONFIG -s -m nokey zimbra_mysql_password`;
    chomp $DB_PASSWORD;
    $DB_USER = `$ZMLOCALCONFIG -m nokey zimbra_mysql_user`;
    chomp $DB_USER;
    $LOGGER_DB_PASSWORD = `$ZMLOCALCONFIG -s -m nokey zimbra_logger_mysql_password`;
    chomp $LOGGER_DB_PASSWORD;
    $MYSQL = "/opt/zimbra/bin/mysql";
}

sub getSchemaVersion {
    my $versionInDb = (runSql("SELECT value FROM config WHERE name = 'db.version'"))[0];
	return $versionInDb;
}

sub getLoggerSchemaVersion {
    my $versionInDb = (runLoggerSql("SELECT value FROM config WHERE name = 'db.version'"))[0];
	return $versionInDb;
}

sub verifySchemaVersion($) {
    my ($version) = @_;
    my $versionInDb = getSchemaVersion();
    if ($version != $versionInDb) {
        print("Schema version mismatch.  Expected version $version.  Version in the database is $versionInDb.\n");
        exit(1);
    }
    Migrate::log("Verified schema version $version.");
}

sub verifyLoggerSchemaVersion($) {
    my ($version) = @_;
    my $versionInDb = getLoggerSchemaVersion();
    if ($version != $versionInDb) {
        print("Schema version mismatch.  Expected version $version.  Version in the database is $versionInDb.\n");
        exit(1);
    }
    Migrate::log("Verified schema version $version.");
}

sub updateLoggerSchemaVersion($$) {
    my ($oldVersion, $newVersion) = @_;
    verifyLoggerSchemaVersion($oldVersion);

    my $sql = <<SET_SCHEMA_VERSION_EOF;
UPDATE zimbra_logger.config SET value = '$newVersion' WHERE name = 'db.version';
SET_SCHEMA_VERSION_EOF

    Migrate::log("Updating logger DB schema version from $oldVersion to $newVersion.");
    runLoggerSql($sql);
}

sub updateSchemaVersion($$) {
    my ($oldVersion, $newVersion) = @_;
    verifySchemaVersion($oldVersion);

    my $sql = <<SET_SCHEMA_VERSION_EOF;
UPDATE $DATABASE.config SET value = '$newVersion' WHERE name = 'db.version';
SET_SCHEMA_VERSION_EOF

    Migrate::log("Updating DB schema version from $oldVersion to $newVersion.");
    runSql($sql);
}

sub getMailboxIds() {
    return runSql("SELECT id FROM mailbox ORDER BY id");
}

sub getMailboxGroups() {
    return runSql("SHOW DATABASES LIKE 'mboxgroup%'");
}

sub runSql(@) {
    my ($script, $logScript) = @_;

    if (! defined($logScript)) {
	    $logScript = 0;
    }

    # Write the last script to a text file for debugging
    # open(LASTSCRIPT, ">lastScript.sql") || die "Could not open lastScript.sql";
    # print(LASTSCRIPT $script);
    # close(LASTSCRIPT);

    if ($logScript) {
	Migrate::log($script);
    }

    # Run the mysql command and redirect output to a temp file
    my $tempFile = "/tmp/mysql.out.$$";
    my $command = "$MYSQL --user=$DB_USER --password=$DB_PASSWORD " .
        "--database=$DATABASE --batch --skip-column-names";
    open(MYSQL, "| $command > $tempFile") || die "Unable to run $command";
    print(MYSQL $script);
    close(MYSQL);

    if ($? != 0) {
        die "Error while running '$command'.";
    }

    # Process output
    open(OUTPUT, $tempFile) || die "Could not open $tempFile";
    my @output;
    while (<OUTPUT>) {
        s/\s+$//;
        push(@output, $_);
    }

    unlink($tempFile);
    return @output;
}

sub runLoggerSql(@) {
    my ($script, $logScript) = @_;

    if (! defined($logScript)) {
	$logScript = 1;
    }

    # Write the last script to a text file for debugging
    # open(LASTSCRIPT, ">lastScript.sql") || die "Could not open lastScript.sql";
    # print(LASTSCRIPT $script);
    # close(LASTSCRIPT);

    if ($logScript) {
	Migrate::log($script);
    }

    # Run the mysql command and redirect output to a temp file
    my $tempFile = "/tmp/mysql.out.$$";
    my $command = "$LOGMYSQL --user=$DB_USER --password=$LOGGER_DB_PASSWORD " .
        "--database=$LOGGER_DATABASE --batch --skip-column-names";
    open(MYSQL, "| $command > $tempFile") || die "Unable to run $command";
    print(MYSQL $script);
    close(MYSQL);

    my @output;
    if ($? != 0) {
		# Hack for missing config
		push @output, 0;
		return @output;
    }

    # Process output
    open(OUTPUT, $tempFile) || die "Could not open $tempFile";
    while (<OUTPUT>) {
        s/\s+$//;
        push(@output, $_);
    }

    unlink($tempFile);
    return @output;
}

sub runSqlParallel(@) {
  my ($procs, @statements) = @_;
  my $debug = 0;    # debug output
  my $verbose = 0;  # incremental verbage
  my $progress = 1; # output little .'s
  my $quiet = 0;    # no output (overrides verbose but not debug)
  my $started = 0;  # internal counter
  my $finished = 0; # internal counter
  my $running = 0;  # internal counter
  my $prog_cnt = 0; # internal counter
  my $timeout = 30;   # alarm timeout
  my $delay = 0;   # delay x seconds before launching new command;
  $procs = 1 unless $procs;    # number of simultaneous connections
  my ($progress_cnt, $numItems);
  
  $quiet = 1 if $progress;
  $verbose = 0 if ($quiet);
  my %pids; 
  if ($debug || $verbose gt 1) {
    print "Timeout  => $timeout\n";
    print "verbose  => $verbose\n";
    print "debug    => $debug\n";
    print "items    => ", scalar@statements, "\n";
    print "procs    => $procs\n";
  }

  foreach my $statement (@statements) {
    next if $statement eq "";
    chomp($statement);
    $prog_cnt = &progress(scalar @statements, $prog_cnt);
  
    print "Forking: \"$statement\"\n" if $verbose;
    # The child process, core here.
    unless ($pids{$statement} = fork()) {
      # set an alarm in case the command hangs.
      $SIG{ALRM} = sub { &alarm_handler($statement,$timeout,$quiet) };
      alarm($timeout);
      my $data_source = "dbi:mysql:database=$DATABASE;mysql_read_default_file=/opt/zimbra/conf/my.cnf;mysql_socket=/opt/zimbra/db/mysql.sock";
      my $dbh;
      until ($dbh) {
        $dbh = DBI->connect($data_source, $DB_USER, $DB_PASSWORD, { PrintError => 0 }); 
        sleep 1;
      }
      unless ($dbh->do($statement) ) {
        warn "DB: $statement: $DBI::errstr\n";
        exit 1;
      }
      $dbh->disconnect;
  
      # execute the statement
      alarm(0);
      exit;
    }
    ++$started;
    $running = $started - $finished;
    if ($running >= $procs) {
      $finished++ if (&mywaiter);
    }
    sleep($delay) if $delay;
  }
  until ($finished >= $started) {
    print "Final wait: Finished $finished of $started\n" if $verbose;
    $finished++ if (&mywaiter);
    sleep 1;
  }
  print "\n" if $progress;
}


sub alarm_handler {
  my ($item,$to,$q) = $_[0];
  print "$item => Cmd exceeded $to seconds.\n" unless $q;
  exit 1;
}

sub mywaiter {
  my ($pid,$exit_value, $signal_num, $dumped_core);
  $pid = wait;
  return undef if ($pid == -1);

  $exit_value = $? >> 8;
  $signal_num = $? & 127;
  $dumped_core = $? & 128;

  die unless $exit_value == 0;
  if (defined $exit_value) {
    return 1;
  } else {
    return 0;
  }
}

sub progress($$) {
  my ($total, $current) = @_;
  my $norm = $total/80;

  $current++;
  if ($current >= $norm) {
    print ".";
    $current = 0;
  }
  return $current;
}
   

sub log
{
    print scalar(localtime()), ": ", @_, "\n";
}

1;
