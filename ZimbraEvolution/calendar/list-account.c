#include <config.h>
#include <gconf/gconf-client.h>
#include <glib/gmain.h>
#include <libedataserver/e-source-list.h>

static GConfClient *conf_client;
static GMainLoop *main_loop;
static char *arg_hostname, *arg_username;
static char * arg_port;
static gboolean arg_useSSL;

#define ZIMBRA_CALDAV_URI_PREFIX		"caldav://"
#define ZIMBRA_CALDAV_PREFIX_LENGTH		9
#define ZIMBRA_URI_PREFIX				"zimbra://"
#define ZIMBRA_PREFIX_LENGTH 			9
#define PARENT_TYPE						G_TYPE_OBJECT

#define CALENDAR_SOURCES "/apps/evolution/addressbook/sources"
#define SELECTED_CALENDARS "/apps/evolution/calendar/display/selected_calendars"

static void 
list_esource (const char *conf_key)
{
	ESourceList *list;
        ESourceGroup *group;
        ESource *source;
        GSList *groups;
        GSList *sources;
	gboolean found_group;
	GConfClient* client;
	GSList *ids;
	GSList *node_tobe_deleted;
	char *source_selection_key;
                                                                                                                             
        client = gconf_client_get_default();
        list = e_source_list_new_for_gconf (client, conf_key);

	group = e_source_list_peek_group_by_name( list, "scott.herscher@dogfood.zimbra.com" );

	if ( group )
	{
char * name = e_source_group_peek_name( group );
fprintf( stderr, "peeked group name = %s\n", name );
	}

	groups = e_source_list_peek_groups (list); 

	for ( ; groups != NULL; groups = g_slist_next (groups))
	{
		group = E_SOURCE_GROUP (groups->data);

char * name = e_source_group_peek_name( group );
fprintf( stderr, "group name = %s\n", name );
char * buri = e_source_group_peek_base_uri( group );
fprintf( stderr, "group base uri = %s\n", buri );
		
		sources = e_source_group_peek_sources (group);
			
		for ( ; sources != NULL; sources = g_slist_next (sources))
		{
			source = E_SOURCE (sources->data);

			buri = e_source_peek_relative_uri (source);

fprintf( stderr, "source relative uri = %s\n", buri );

			char * xml = e_source_to_standalone_xml( source );

fprintf( stderr, "xml = %s\n", xml );
		}
	}

	g_object_unref (list);
	g_object_unref (client);		
}


static void 
list_account(const char *conf_key )
{
	list_esource( conf_key );
}


static gboolean
idle_cb (gpointer data)
{
	list_account ("/apps/evolution/addressbook/sources" );

	g_main_loop_quit (main_loop);

	return FALSE;
}

int
main (int argc, char *argv[])
{
	gboolean bool;

	g_type_init ();

	if (argc != 1 ) {
		g_print ("Usage: %s\n" );
		return -1;
	}

	conf_client = gconf_client_get_default ();

	main_loop = g_main_loop_new (NULL, TRUE);
	g_idle_add ((GSourceFunc) idle_cb, NULL);
	g_main_loop_run (main_loop);

	/* terminate */
	g_object_unref (conf_client);
	g_main_loop_unref (main_loop);

	return 0;
}
