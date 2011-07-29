package com.zimbra.qa.selenium.projects.desktop.tests.accounts;

import java.util.List;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.zimbra.qa.selenium.framework.items.DesktopAccountItem;
import com.zimbra.qa.selenium.framework.items.FolderItem;
import com.zimbra.qa.selenium.framework.items.DesktopAccountItem.SECURITY_TYPE;
import com.zimbra.qa.selenium.framework.ui.Button;
import com.zimbra.qa.selenium.framework.util.HarnessException;
import com.zimbra.qa.selenium.framework.util.ZAssert;
import com.zimbra.qa.selenium.framework.util.ZimbraAccount;
import com.zimbra.qa.selenium.framework.util.ZimbraSeleniumProperties;
import com.zimbra.qa.selenium.framework.util.staf.Stafzmtlsctl;
import com.zimbra.qa.selenium.framework.util.staf.Stafzmtlsctl.SERVER_ACCESS;
import com.zimbra.qa.selenium.projects.desktop.core.AjaxCommonTest;
import com.zimbra.qa.selenium.projects.desktop.ui.PageLogin;
import com.zimbra.qa.selenium.projects.desktop.ui.accounts.FormAddGmailAccount;
import com.zimbra.qa.selenium.projects.desktop.ui.accounts.FormAddImapAccount;
import com.zimbra.qa.selenium.projects.desktop.ui.accounts.FormAddPopAccount;
import com.zimbra.qa.selenium.projects.desktop.ui.accounts.FormAddYahooAccount;
import com.zimbra.qa.selenium.projects.desktop.ui.accounts.FormAddZimbraAccount;
import com.zimbra.qa.selenium.projects.desktop.ui.accounts.PageAddNewAccount.DROP_DOWN_OPTION;

public class CreateAccount extends AjaxCommonTest {

   private boolean _sslIsModified = false;

   @BeforeMethod(alwaysRun=true)
   public void setup() {
      _sslIsModified = false;
   }

   public CreateAccount() {
      logger.info("New " + CreateAccount.class.getCanonicalName());

      super.startingPage = app.zPageAddNewAccount;
      super.startingAccountPreferences = null;
   }

   @Test(description="Create New Single Account (Zimbra) - SSL", groups = { "sanity" })
   public void CreateSingleZimbraAccountSSL() throws HarnessException{
      Stafzmtlsctl stafzmtlsctl = new Stafzmtlsctl();
      stafzmtlsctl.setServerAccess(SERVER_ACCESS.BOTH);
      _sslIsModified = true;

      DesktopAccountItem desktopAccountItem = app.zPageAddNewAccount.zAddZimbraAccountThruUI(ZimbraAccount.AccountZWC().EmailAddress,
            ZimbraAccount.AccountZWC().Password, true, "443");

      String message = app.zPageLogin.zGetMessage();
      ZAssert.assertStringContains(message, "Account added: " + desktopAccountItem.accountName, "Verify Account added message");

      app.zPageLogin.zLogin(new ZimbraAccount(desktopAccountItem.emailAddress,
            desktopAccountItem.password));
      List<FolderItem> folders = app.zTreeMail.zListGetFolders();
      ZAssert.assertGreaterThan(folders.size(), 0, "Folder with the active account's email address is greater than 0.");

   }

   @Test(description="Create New Single Account (Zimbra) - Non SSL", groups = { "sanity" })
   public void CreateSingleZimbraAccountNonSSL() throws HarnessException {

      DesktopAccountItem desktopAccountItem = app.zPageAddNewAccount.zAddZimbraAccountThruUI();

      String message = app.zPageLogin.zGetMessage();
      ZAssert.assertStringContains(message, "Account added: " + desktopAccountItem.accountName, "Verify Account added message");

      app.zPageLogin.zLogin(new ZimbraAccount(desktopAccountItem.emailAddress,
            desktopAccountItem.password));
      List<FolderItem> folders = app.zTreeMail.zListGetFolders();
      ZAssert.assertGreaterThan(folders.size(), 0, "Folder with the active account's email address is greater than 0.");
   }

   @Test(description="Create Multiple Accounts (Zimbra) - SSL", groups = { "functional" })
   public void CreateMultipleZimbraAccountSSL() throws HarnessException {
      Stafzmtlsctl stafzmtlsctl = new Stafzmtlsctl();
      stafzmtlsctl.setServerAccess(SERVER_ACCESS.BOTH);
      _sslIsModified = true;

      DesktopAccountItem desktopAccountItem = app.zPageAddNewAccount.zAddZimbraAccountThruUI(ZimbraAccount.AccountZWC().EmailAddress,
            ZimbraAccount.AccountZWC().Password, true, "443");

      String message = app.zPageLogin.zGetMessage();
      ZAssert.assertStringContains(message, "Account added: " + desktopAccountItem.accountName, "Verify Account added message");

      DesktopAccountItem desktopAccountItem2 = app.zPageAddNewAccount.zAddZimbraAccountThruUI(ZimbraAccount.AccountA().EmailAddress,
            ZimbraAccount.AccountA().Password, true, "443");

      message = app.zPageLogin.zGetMessage();
      ZAssert.assertStringContains(message, "Account added: " + desktopAccountItem2.accountName, "Verify Account added message");

      app.zPageLogin.zLogin(new ZimbraAccount(desktopAccountItem.emailAddress,
            desktopAccountItem.password));
      List<FolderItem> folders = app.zTreeMail.zListGetFolders();
      ZAssert.assertGreaterThan(folders.size(), 0, "Folder with the active account1's email address is greater than 0.");

      app.zSetActiveAcount(new ZimbraAccount(desktopAccountItem2.emailAddress,
            desktopAccountItem2.password));
      folders = app.zTreeMail.zListGetFolders();
      ZAssert.assertGreaterThan(folders.size(), 0, "Folder with the active account2's email address is greater than 0.");
   }

   @Test(description="Create Multiple Accounts (Zimbra) - Non SSL", groups = { "functional" })
   public void CreateMultipleZimbraAccountNonSSL() throws HarnessException {

      DesktopAccountItem desktopAccountItem = app.zPageAddNewAccount.zAddZimbraAccountThruUI(ZimbraAccount.AccountZWC().EmailAddress,
            ZimbraAccount.AccountZWC().Password, false, "80");

      String message = app.zPageLogin.zGetMessage();
      ZAssert.assertStringContains(message, "Account added: " + desktopAccountItem.accountName, "Verify Account added message");

      DesktopAccountItem desktopAccountItem2 = app.zPageAddNewAccount.zAddZimbraAccountThruUI(ZimbraAccount.AccountA().EmailAddress,
            ZimbraAccount.AccountA().Password, false, "80");

      message = app.zPageLogin.zGetMessage();
      ZAssert.assertStringContains(message, "Account added: " + desktopAccountItem2.accountName, "Verify Account added message");

      app.zPageLogin.zLogin(new ZimbraAccount(desktopAccountItem.emailAddress,
            desktopAccountItem.password));
      List<FolderItem> folders = app.zTreeMail.zListGetFolders();
      ZAssert.assertGreaterThan(folders.size(), 0, "Folder with the active account1's email address is greater than 0.");

      app.zSetActiveAcount(new ZimbraAccount(desktopAccountItem2.emailAddress,
            desktopAccountItem2.password));
      folders = app.zTreeMail.zListGetFolders();
      ZAssert.assertGreaterThan(folders.size(), 0, "Folder with the active account2's email address is greater than 0.");
   }

   @Test(description="Add Yahoo account to ZD client", groups = { "sanity" })
   public void addYahooAccount() throws HarnessException {

      DesktopAccountItem desktopAccountItem = app.zPageAddNewAccount.zAddYahooAccountThruUI();

      String message = app.zPageLogin.zGetMessage();
      ZAssert.assertStringContains(message, "Account added: " + desktopAccountItem.accountName, "Verify Account added message");

      app.zPageLogin.zLogin(new ZimbraAccount(desktopAccountItem.emailAddress,
            desktopAccountItem.password));
      List<FolderItem> folders = app.zTreeMail.zListGetFolders();
      ZAssert.assertGreaterThan(folders.size(), 0, "Folder with the active account's email address is greater than 0.");
   }

   @Test(description="Add Gmail account to ZD client", groups = { "sanity" })
   public void addGmailAccount() throws HarnessException {

      DesktopAccountItem desktopAccountItem = app.zPageAddNewAccount.zAddGmailAccountThruUI();

      String message = app.zPageLogin.zGetMessage();
      ZAssert.assertStringContains(message, "Account added: " + desktopAccountItem.accountName, "Verify Account added message");

      app.zPageLogin.zLogin(new ZimbraAccount(desktopAccountItem.emailAddress,
            desktopAccountItem.password));
      List<FolderItem> folders = app.zTreeMail.zListGetFolders();
      ZAssert.assertGreaterThan(folders.size(), 0, "Folder with the active account's email address is greater than 0.");
   }

   @Test(description="Add Gmail IMAP account to ZD client", groups = { "private" })
   public void addGmailImapAccount() throws HarnessException {

      DesktopAccountItem desktopAccountItem = app.zPageAddNewAccount.zAddGmailImapAccountThruUI();

      String message = app.zPageLogin.zGetMessage();
      ZAssert.assertStringContains(message, "Account added: " + desktopAccountItem.accountName, "Verify Account added message");

      app.zPageLogin.zLogin(new ZimbraAccount(desktopAccountItem.emailAddress,
            desktopAccountItem.password));
      List<FolderItem> folders = app.zTreeMail.zListGetFolders();
      ZAssert.assertGreaterThan(folders.size(), 0, "Folder with the active account's email address is greater than 0.");
   }

   @Test(description="Add Zimbra IMAP (SSL) account to ZD client with Sending Mail set to SSL", groups = { "smoke" })
   public void addZimbraImapSslSendingSslAccount() throws HarnessException {
      Stafzmtlsctl stafzmtlsctl = new Stafzmtlsctl();
      stafzmtlsctl.setServerAccess(SERVER_ACCESS.BOTH);
      _sslIsModified = true;

      DesktopAccountItem desktopAccountItem = app.zPageAddNewAccount.zAddZimbraImapAccountThruUI(ZimbraAccount.AccountZWC().EmailAddress,
            ZimbraAccount.AccountZWC().Password,
            ZimbraSeleniumProperties.getStringProperty("server.host", "localhost"),
            true,
            "465");

      String message = app.zPageLogin.zGetMessage();
      ZAssert.assertStringContains(message, "Account added: " + desktopAccountItem.accountName, "Verify Account added message");

      app.zPageLogin.zLogin(new ZimbraAccount(desktopAccountItem.emailAddress,
            desktopAccountItem.password));
      List<FolderItem> folders = app.zTreeMail.zListGetFolders();
      ZAssert.assertGreaterThan(folders.size(), 0, "Folder with the active account's email address is greater than 0.");
   }

   @Test(description="Add Zimbra IMAP (SSL) account to ZD client with Sending Mail set to non SSL", groups = { "smoke" })
   public void addZimbraImapSslSendingNonSslAccount() throws HarnessException {
      Stafzmtlsctl stafzmtlsctl = new Stafzmtlsctl();
      stafzmtlsctl.setServerAccess(SERVER_ACCESS.BOTH);
      _sslIsModified = true;

      DesktopAccountItem desktopAccountItem = app.zPageAddNewAccount.zAddZimbraImapAccountThruUI(ZimbraAccount.AccountZWC().EmailAddress,
            ZimbraAccount.AccountZWC().Password,
            ZimbraSeleniumProperties.getStringProperty("server.host", "localhost"),
            false,
            "25");

      String message = app.zPageLogin.zGetMessage();
      ZAssert.assertStringContains(message, "Account added: " + desktopAccountItem.accountName, "Verify Account added message");

      app.zPageLogin.zLogin(new ZimbraAccount(desktopAccountItem.emailAddress,
            desktopAccountItem.password));
      List<FolderItem> folders = app.zTreeMail.zListGetFolders();
      ZAssert.assertGreaterThan(folders.size(), 0, "Folder with the active account's email address is greater than 0.");
   }

   @Test(description="Failure in adding Zimbra IMAP account with Receiving Mail security set to None",
         groups = { "functional" })
   public void addZimbraImapNonSslAccount() throws HarnessException {

      DesktopAccountItem desktopAccountItem = DesktopAccountItem.generateDesktopImapAccountItem(
            ZimbraAccount.AccountZWC().EmailAddress,
            ZimbraAccount.AccountZWC().EmailAddress,
            ZimbraAccount.AccountZWC().Password,
            ZimbraSeleniumProperties.getStringProperty("server.host", "localhost"),
            SECURITY_TYPE.NONE,
            null,
            ZimbraSeleniumProperties.getStringProperty("server.host", "localhost"),
            false,
            null,
            ZimbraAccount.AccountZWC().EmailAddress,
            ZimbraAccount.AccountZWC().Password);

      FormAddImapAccount accountForm = (FormAddImapAccount)app.
            zPageAddNewAccount.zDropDownListSelect(DROP_DOWN_OPTION.IMAP);
      accountForm.zFill(desktopAccountItem);

      accountForm.zPressButton(Button.B_VALIDATE_AND_SAVE);

      String message = app.zPageLogin.zGetMessage();
      ZAssert.assertStringContains(message,
            "User account authentication failed. Please check username and password.",
            "Verify error message of disabled cleartext login");

      app.zPageLogin.zNavigateTo();

      String welcomeMessage = app.zPageLogin.zGetWelcomeMessage();
      ZAssert.assertStringContains(welcomeMessage,
            "Zimbra Desktop allows you to access email while you are disconnected from the internet.",
            "Verify welcome message is displayed");

      ZAssert.assertEquals(false,
            app.zPageLogin.sIsElementPresent(PageLogin.Locators.zDisplayedMessage),
            "Added account message is displayed");
   }

   @Test(description="Add Zimbra POP (SSL) account to ZD client with Sending Mail set to SSL", groups = { "smoke" })
   public void addZimbraPopSslSendingSslAccount() throws HarnessException {
      Stafzmtlsctl stafzmtlsctl = new Stafzmtlsctl();
      stafzmtlsctl.setServerAccess(SERVER_ACCESS.BOTH);
      _sslIsModified = true;

      DesktopAccountItem desktopAccountItem = app.zPageAddNewAccount.zAddZimbraPopAccountThruUI(ZimbraAccount.AccountZWC().EmailAddress,
            ZimbraAccount.AccountZWC().Password,
            ZimbraSeleniumProperties.getStringProperty("server.host", "localhost"),
            true,
            "465");

      String message = app.zPageLogin.zGetMessage();
      ZAssert.assertStringContains(message, "Account added: " + desktopAccountItem.accountName, "Verify Account added message");

      app.zPageLogin.zLogin(new ZimbraAccount(desktopAccountItem.emailAddress,
            desktopAccountItem.password));
      List<FolderItem> folders = app.zTreeMail.zListGetFolders();
      ZAssert.assertGreaterThan(folders.size(), 0, "Folder with the active account's email address is greater than 0.");
   }

   @Test(description="Add Zimbra POP (SSL) account to ZD client with Sending Mail set to non SSL", groups = { "smoke" })
   public void addZimbraPopSslSendingNonSslAccount() throws HarnessException {
      Stafzmtlsctl stafzmtlsctl = new Stafzmtlsctl();
      stafzmtlsctl.setServerAccess(SERVER_ACCESS.BOTH);
      _sslIsModified = true;

      DesktopAccountItem desktopAccountItem = app.zPageAddNewAccount.zAddZimbraPopAccountThruUI(ZimbraAccount.AccountZWC().EmailAddress,
            ZimbraAccount.AccountZWC().Password,
            ZimbraSeleniumProperties.getStringProperty("server.host", "localhost"),
            false,
            "25");

      String message = app.zPageLogin.zGetMessage();
      ZAssert.assertStringContains(message, "Account added: " + desktopAccountItem.accountName, "Verify Account added message");

      app.zPageLogin.zLogin(new ZimbraAccount(desktopAccountItem.emailAddress,
            desktopAccountItem.password));
      List<FolderItem> folders = app.zTreeMail.zListGetFolders();
      ZAssert.assertGreaterThan(folders.size(), 0, "Folder with the active account's email address is greater than 0.");
   }

   @Test(description="Failure in adding Zimbra POP account with Receiving Mail security set to None",
         groups = { "functional" })
   public void addZimbraPopNonSslAccount() throws HarnessException {

      DesktopAccountItem desktopAccountItem = DesktopAccountItem.generateDesktopPopAccountItem(
            ZimbraAccount.AccountZWC().EmailAddress,
            ZimbraAccount.AccountZWC().EmailAddress,
            ZimbraAccount.AccountZWC().Password,
            ZimbraSeleniumProperties.getStringProperty("server.host", "localhost"),
            SECURITY_TYPE.NONE,
            null,
            ZimbraSeleniumProperties.getStringProperty("server.host", "localhost"),
            false,
            null,
            ZimbraAccount.AccountZWC().EmailAddress,
            ZimbraAccount.AccountZWC().Password);

      FormAddPopAccount accountForm = (FormAddPopAccount)app.
            zPageAddNewAccount.zDropDownListSelect(DROP_DOWN_OPTION.POP);
      accountForm.zFill(desktopAccountItem);

      accountForm.zPressButton(Button.B_VALIDATE_AND_SAVE);

      String message = app.zPageLogin.zGetMessage();
      ZAssert.assertStringContains(message,
            "User account authentication failed. Please check username and password.",
            "Verify error message of disabled cleartext login");

      app.zPageLogin.zNavigateTo();

      String welcomeMessage = app.zPageLogin.zGetWelcomeMessage();
      ZAssert.assertStringContains(welcomeMessage,
            "Zimbra Desktop allows you to access email while you are disconnected from the internet.",
            "Verify welcome message is displayed");

      ZAssert.assertEquals(false,
            app.zPageLogin.sIsElementPresent(PageLogin.Locators.zDisplayedMessage),
            "Added account message is displayed");
   }

   @Test(description="Add Hotmail POP account to ZD client", groups = { "private" })
   public void addHotmailPopAccount() throws HarnessException {

      DesktopAccountItem desktopAccountItem = app.zPageAddNewAccount.zAddPopAccountThruUI();

      String message = app.zPageLogin.zGetMessage();
      ZAssert.assertStringContains(message, "Account added: " + desktopAccountItem.accountName, "Verify Account added message");

      app.zPageLogin.zLogin(new ZimbraAccount(desktopAccountItem.emailAddress,
            desktopAccountItem.password));
      List<FolderItem> folders = app.zTreeMail.zListGetFolders();
      ZAssert.assertGreaterThan(folders.size(), 0, "Folder with the active account's email address is greater than 0.");
   }

   @Test(description="Wrong email address format (alphabet characters only) when creating Zimbra Account", groups = { "functional" } )
   public void wrongEmailAddressFormatZimbraAccount1() throws HarnessException {

      String wrongEmailAddress = ZimbraSeleniumProperties.getUniqueString();
      DesktopAccountItem desktopAccountItem = DesktopAccountItem.generateDesktopZimbraAccountItem(
            wrongEmailAddress,
            ZimbraAccount.AccountZWC().Password,
            ZimbraSeleniumProperties.getStringProperty("server.port", "80"),
            false);

      FormAddZimbraAccount accountForm = (FormAddZimbraAccount)app.zPageAddNewAccount.zDropDownListSelect(DROP_DOWN_OPTION.ZIMBRA);
      accountForm.zFill(desktopAccountItem);
      accountForm.zPressButton(Button.B_VALIDATE_AND_SAVE);

      String message = app.zPageLogin.zGetMessage();
      ZAssert.assertStringContains(message,
            "Please correct missing or invalid input.",
            "Verify error message of wrong email address format");

      app.zPageLogin.zNavigateTo();

      String welcomeMessage = app.zPageLogin.zGetWelcomeMessage();
      ZAssert.assertStringContains(welcomeMessage,
            "Zimbra Desktop allows you to access email while you are disconnected from the internet.",
            "Verify welcome message is displayed");

      ZAssert.assertEquals(false,
            app.zPageLogin.sIsElementPresent(PageLogin.Locators.zDisplayedMessage),
            "Added account message is displayed");
   }

   @Test(description="Wrong email address format (alphabet characters and '@') when creating Zimbra Account", groups = { "functional" } )
   public void wrongEmailAddressFormatZimbraAccount2() throws HarnessException {

      String wrongEmailAddress = ZimbraSeleniumProperties.getUniqueString() + "@";
      DesktopAccountItem desktopAccountItem = DesktopAccountItem.generateDesktopZimbraAccountItem(
            wrongEmailAddress,
            ZimbraAccount.AccountZWC().Password,
            ZimbraSeleniumProperties.getStringProperty("server.port", "80"),
            false);

      FormAddZimbraAccount accountForm = (FormAddZimbraAccount)app.zPageAddNewAccount.zDropDownListSelect(DROP_DOWN_OPTION.ZIMBRA);
      accountForm.zFill(desktopAccountItem);
      accountForm.zPressButton(Button.B_VALIDATE_AND_SAVE);

      String message = app.zPageLogin.zGetMessage();
      ZAssert.assertStringContains(message,
            "Please correct missing or invalid input.",
            "Verify error message of wrong email address format");

      app.zPageLogin.zNavigateTo();

      String welcomeMessage = app.zPageLogin.zGetWelcomeMessage();
      ZAssert.assertStringContains(welcomeMessage,
            "Zimbra Desktop allows you to access email while you are disconnected from the internet.",
            "Verify welcome message is displayed");

      ZAssert.assertEquals(false,
            app.zPageLogin.sIsElementPresent(PageLogin.Locators.zDisplayedMessage),
            "Added account message is displayed");
   }

   @Test(description="Wrong email address format (alphabet characters) when creating IMAP Account", groups = { "functional" } )
   public void wrongEmailAddressFormatImapAccount1() throws HarnessException {
      String wrongEmailAddress = ZimbraSeleniumProperties.getUniqueString();
      DesktopAccountItem desktopAccountItem = DesktopAccountItem.generateDesktopImapAccountItem(
            wrongEmailAddress,
            AjaxCommonTest.gmailUserName,
            AjaxCommonTest.gmailPassword,
            AjaxCommonTest.gmailImapReceivingServer,
            SECURITY_TYPE.SSL,
            "993",
            AjaxCommonTest.gmailImapSmtpServer,
            true,
            "465",
            AjaxCommonTest.gmailUserName,
            AjaxCommonTest.gmailPassword);

      FormAddImapAccount accountForm = (FormAddImapAccount)app.zPageAddNewAccount.zDropDownListSelect(DROP_DOWN_OPTION.IMAP);
      accountForm.zFill(desktopAccountItem);
      accountForm.zPressButton(Button.B_VALIDATE_AND_SAVE);

      String message = app.zPageLogin.zGetMessage();
      ZAssert.assertStringContains(message,
            "Please correct missing or invalid input.",
            "Verify error message of wrong email address format");

      app.zPageLogin.zNavigateTo();

      String welcomeMessage = app.zPageLogin.zGetWelcomeMessage();
      ZAssert.assertStringContains(welcomeMessage,
            "Zimbra Desktop allows you to access email while you are disconnected from the internet.",
            "Verify welcome message is displayed");

      ZAssert.assertEquals(false,
            app.zPageLogin.sIsElementPresent(PageLogin.Locators.zDisplayedMessage),
            "Added account message is displayed");
   }

   @Test(description="Wrong email address format (alphabet characters and '@') when creating IMAP Account", groups = { "functional" } )
   public void wrongEmailAddressFormatImapAccount2() throws HarnessException {
      String wrongEmailAddress = ZimbraSeleniumProperties.getUniqueString() + "@";
      DesktopAccountItem desktopAccountItem = DesktopAccountItem.generateDesktopImapAccountItem(
            wrongEmailAddress,
            AjaxCommonTest.gmailUserName,
            AjaxCommonTest.gmailPassword,
            AjaxCommonTest.gmailImapReceivingServer,
            SECURITY_TYPE.SSL,
            "993",
            AjaxCommonTest.gmailImapSmtpServer,
            true,
            "465",
            AjaxCommonTest.gmailUserName,
            AjaxCommonTest.gmailPassword);

      FormAddImapAccount accountForm = (FormAddImapAccount)app.zPageAddNewAccount.zDropDownListSelect(DROP_DOWN_OPTION.IMAP);
      accountForm.zFill(desktopAccountItem);
      accountForm.zPressButton(Button.B_VALIDATE_AND_SAVE);

      String message = app.zPageLogin.zGetMessage();
      ZAssert.assertStringContains(message,
            "Please correct missing or invalid input.",
            "Verify error message of wrong email address format");

      app.zPageLogin.zNavigateTo();

      String welcomeMessage = app.zPageLogin.zGetWelcomeMessage();
      ZAssert.assertStringContains(welcomeMessage,
            "Zimbra Desktop allows you to access email while you are disconnected from the internet.",
            "Verify welcome message is displayed");

      ZAssert.assertEquals(false,
            app.zPageLogin.sIsElementPresent(PageLogin.Locators.zDisplayedMessage),
            "Added account message is displayed");
   }

   @Test(description="Wrong email address format (alphabet characters) when creating POP Account", groups = { "functional" } )
   public void wrongEmailAddressFormatPopAccount1() throws HarnessException {
      String wrongEmailAddress = ZimbraSeleniumProperties.getUniqueString();
      DesktopAccountItem desktopAccountItem = DesktopAccountItem.generateDesktopImapAccountItem(
            wrongEmailAddress,
            AjaxCommonTest.hotmailUserName,
            AjaxCommonTest.hotmailPassword,
            AjaxCommonTest.hotmailPopReceivingServer,
            SECURITY_TYPE.SSL,
            "995",
            AjaxCommonTest.hotmailPopSmtpServer,
            false,
            "25",
            AjaxCommonTest.hotmailUserName,
            AjaxCommonTest.hotmailPassword);

      FormAddPopAccount accountForm = (FormAddPopAccount)app.zPageAddNewAccount.zDropDownListSelect(DROP_DOWN_OPTION.POP);
      accountForm.zFill(desktopAccountItem);
      accountForm.zPressButton(Button.B_VALIDATE_AND_SAVE);

      String message = app.zPageLogin.zGetMessage();
      ZAssert.assertStringContains(message,
            "Please correct missing or invalid input.",
            "Verify error message of wrong email address format");

      app.zPageLogin.zNavigateTo();

      String welcomeMessage = app.zPageLogin.zGetWelcomeMessage();
      ZAssert.assertStringContains(welcomeMessage,
            "Zimbra Desktop allows you to access email while you are disconnected from the internet.",
            "Verify welcome message is displayed");

      ZAssert.assertEquals(false,
            app.zPageLogin.sIsElementPresent(PageLogin.Locators.zDisplayedMessage),
            "Added account message is displayed");
   }

   @Test(description="Wrong email address format (alphabet characters and '@') when creating POP Account", groups = { "functional" } )
   public void wrongEmailAddressFormatPopAccount2() throws HarnessException {
      String wrongEmailAddress = ZimbraSeleniumProperties.getUniqueString() + "@";
      DesktopAccountItem desktopAccountItem = DesktopAccountItem.generateDesktopImapAccountItem(
            wrongEmailAddress,
            AjaxCommonTest.hotmailUserName,
            AjaxCommonTest.hotmailPassword,
            AjaxCommonTest.hotmailPopReceivingServer,
            SECURITY_TYPE.SSL,
            "995",
            AjaxCommonTest.hotmailPopSmtpServer,
            false,
            "25",
            AjaxCommonTest.hotmailUserName,
            AjaxCommonTest.hotmailPassword);

      FormAddPopAccount accountForm = (FormAddPopAccount)app.zPageAddNewAccount.zDropDownListSelect(DROP_DOWN_OPTION.POP);
      accountForm.zFill(desktopAccountItem);
      accountForm.zPressButton(Button.B_VALIDATE_AND_SAVE);

      String message = app.zPageLogin.zGetMessage();
      ZAssert.assertStringContains(message,
            "Please correct missing or invalid input.",
            "Verify error message of wrong email address format");

      app.zPageLogin.zNavigateTo();

      String welcomeMessage = app.zPageLogin.zGetWelcomeMessage();
      ZAssert.assertStringContains(welcomeMessage,
            "Zimbra Desktop allows you to access email while you are disconnected from the internet.",
            "Verify welcome message is displayed");

      ZAssert.assertEquals(false,
            app.zPageLogin.sIsElementPresent(PageLogin.Locators.zDisplayedMessage),
            "Added account message is displayed");
   }

   @Test(description="Wrong email address when creating Zimbra Account", groups = { "functional" } )
   public void wrongEmailAddressZimbraAccount() throws HarnessException {

      String wrongEmailAddress = ZimbraSeleniumProperties.getUniqueString() + "@testdomain.com";
      DesktopAccountItem desktopAccountItem = DesktopAccountItem.generateDesktopZimbraAccountItem(
            wrongEmailAddress,
            ZimbraAccount.AccountZWC().Password,
            ZimbraSeleniumProperties.getStringProperty("server.port", "80"),
            false);

      FormAddZimbraAccount accountForm = (FormAddZimbraAccount)app.zPageAddNewAccount.zDropDownListSelect(DROP_DOWN_OPTION.ZIMBRA);
      accountForm.zFill(desktopAccountItem);
      accountForm.zPressButton(Button.B_VALIDATE_AND_SAVE);

      String message = app.zPageLogin.zGetMessage();
      ZAssert.assertStringContains(message,
            "Account authentication failed. Please check username and password.",
            "Verify error message of wrong email address");

      app.zPageLogin.zNavigateTo();

      String welcomeMessage = app.zPageLogin.zGetWelcomeMessage();
      ZAssert.assertStringContains(welcomeMessage,
            "Zimbra Desktop allows you to access email while you are disconnected from the internet.",
            "Verify welcome message is displayed");

      ZAssert.assertEquals(false,
            app.zPageLogin.sIsElementPresent(PageLogin.Locators.zDisplayedMessage),
            "Added account message is displayed");
   }

   @Test(description="Wrong password when creating Zimbra Account", groups = { "functional" } )
   public void wrongPasswordZimbraAccount() throws HarnessException {

      String wrongPassword = ZimbraSeleniumProperties.getUniqueString();
      DesktopAccountItem desktopAccountItem = DesktopAccountItem.generateDesktopZimbraAccountItem(
            ZimbraAccount.AccountZWC().EmailAddress,
            wrongPassword,
            ZimbraSeleniumProperties.getStringProperty("server.port", "80"),
            false);

      FormAddZimbraAccount accountForm = (FormAddZimbraAccount)app.zPageAddNewAccount.zDropDownListSelect(DROP_DOWN_OPTION.ZIMBRA);
      accountForm.zFill(desktopAccountItem);
      accountForm.zPressButton(Button.B_VALIDATE_AND_SAVE);

      String message = app.zPageLogin.zGetMessage();
      ZAssert.assertStringContains(message,
            "Account authentication failed. Please check username and password.",
            "Verify error message of wrong password");

      app.zPageLogin.zNavigateTo();

      String welcomeMessage = app.zPageLogin.zGetWelcomeMessage();
      ZAssert.assertStringContains(welcomeMessage,
            "Zimbra Desktop allows you to access email while you are disconnected from the internet.",
            "Verify welcome message is displayed");

      ZAssert.assertEquals(false,
            app.zPageLogin.sIsElementPresent(PageLogin.Locators.zDisplayedMessage),
            "Added account message is displayed");
   }

   @Test(description="Wrong server when creating Zimbra Account", groups = { "functional" } )
   public void wrongServerZimbraAccount() throws HarnessException {

      String wrongServer = "1.1.1.1";
      DesktopAccountItem desktopAccountItem = DesktopAccountItem.generateDesktopZimbraAccountItem(
            ZimbraAccount.AccountZWC().EmailAddress,
            ZimbraAccount.AccountZWC().Password,
            ZimbraSeleniumProperties.getStringProperty("server.port", "80"),
            wrongServer,
            false);

      FormAddZimbraAccount accountForm = (FormAddZimbraAccount)app.zPageAddNewAccount.zDropDownListSelect(DROP_DOWN_OPTION.ZIMBRA);
      accountForm.zFill(desktopAccountItem);
      accountForm.zPressButton(Button.B_VALIDATE_AND_SAVE);

      String message = app.zPageLogin.zGetMessage(true);
      ZAssert.assertStringContains(message,
            "Timeout when connecting to \"http://" + wrongServer + "/service/soap/\"." +
            		" Please check host/port and network connectivity.",
            "Verify error message of wrong incoming server address");

      app.zPageLogin.zNavigateTo();

      String welcomeMessage = app.zPageLogin.zGetWelcomeMessage();
      ZAssert.assertStringContains(welcomeMessage,
            "Zimbra Desktop allows you to access email while you are disconnected from the internet.",
            "Verify welcome message is displayed");

      ZAssert.assertEquals(false,
            app.zPageLogin.sIsElementPresent(PageLogin.Locators.zDisplayedMessage),
            "Added account message is displayed");
   }

   @Test(description="Wrong Non-SSL port when creating Zimbra Account", groups = { "functional" })
   public void wrongNonSSLPortZimbraAccount() throws HarnessException {
      String nonSSLPort = "111";
      DesktopAccountItem desktopAccountItem = DesktopAccountItem.generateDesktopZimbraAccountItem(
            ZimbraAccount.AccountZWC().EmailAddress,
            ZimbraAccount.AccountZWC().Password,
            nonSSLPort,
            false);

      FormAddZimbraAccount accountForm = (FormAddZimbraAccount)app.zPageAddNewAccount.zDropDownListSelect(DROP_DOWN_OPTION.ZIMBRA);
      accountForm.zFill(desktopAccountItem);
      accountForm.zPressButton(Button.B_VALIDATE_AND_SAVE);

      String message = app.zPageLogin.zGetMessage();
      ZAssert.assertStringContains(message,
            "service.FAILURE: system failure: error while proxying request to target server:" +
            " HTTP/1.0 403 Forbidden Display error details",
            "Verify error message of wrong incoming server address");

      app.zPageLogin.zNavigateTo();

      String welcomeMessage = app.zPageLogin.zGetWelcomeMessage();
      ZAssert.assertStringContains(welcomeMessage,
            "Zimbra Desktop allows you to access email while you are disconnected from the internet.",
            "Verify welcome message is displayed");

      ZAssert.assertEquals(false,
            app.zPageLogin.sIsElementPresent(PageLogin.Locators.zDisplayedMessage),
            "Added account message is displayed");
   }

   @Test(description="Wrong SSL port when creating Zimbra Account", groups = { "functional" })
   public void wrongSSLPortZimbraAccount() throws HarnessException {
      String sslPort = "111";
      DesktopAccountItem desktopAccountItem = DesktopAccountItem.generateDesktopZimbraAccountItem(
            ZimbraAccount.AccountZWC().EmailAddress,
            ZimbraAccount.AccountZWC().Password,
            sslPort,
            true);

      FormAddZimbraAccount accountForm = (FormAddZimbraAccount)app.zPageAddNewAccount.zDropDownListSelect(DROP_DOWN_OPTION.ZIMBRA);
      accountForm.zFill(desktopAccountItem);
      accountForm.zPressButton(Button.B_VALIDATE_AND_SAVE);

      String message = app.zPageLogin.zGetMessage();
      ZAssert.assertStringContains(message,
            "service.FAILURE: system failure: error while proxying request to target server:" +
            " HTTP/1.0 403 Forbidden Display error details",
            "Verify error message of wrong incoming server address");

      app.zPageLogin.zNavigateTo();

      String welcomeMessage = app.zPageLogin.zGetWelcomeMessage();
      ZAssert.assertStringContains(welcomeMessage,
            "Zimbra Desktop allows you to access email while you are disconnected from the internet.",
            "Verify welcome message is displayed");

      ZAssert.assertEquals(false,
            app.zPageLogin.sIsElementPresent(PageLogin.Locators.zDisplayedMessage),
            "Added account message is displayed");
   }

   @Test(description="Wrong email address when creating Gmail Account", groups = { "functional" })
   public void wrongEmailAddressGmailAccount() throws HarnessException {

      String wrongEmailAddress = ZimbraSeleniumProperties.getUniqueString() + "@gmail.com";
      DesktopAccountItem desktopAccountItem = DesktopAccountItem.generateDesktopGmailAccountItem(
            wrongEmailAddress, AjaxCommonTest.gmailPassword);

      FormAddGmailAccount accountForm = (FormAddGmailAccount)app.zPageAddNewAccount.zDropDownListSelect(
            DROP_DOWN_OPTION.GMAIL);
      accountForm.zFill(desktopAccountItem);
      accountForm.zSubmit();

      String message = app.zPageLogin.zGetMessage();
      ZAssert.assertStringContains(message,
            "User account authentication failed. Please check username and password.",
            "Verify error message of wrong password");

      app.zPageLogin.zNavigateTo();

      String welcomeMessage = app.zPageLogin.zGetWelcomeMessage();
      ZAssert.assertStringContains(welcomeMessage,
            "Zimbra Desktop allows you to access email while you are disconnected from the internet.",
            "Verify welcome message is displayed");

      ZAssert.assertEquals(false,
            app.zPageLogin.sIsElementPresent(PageLogin.Locators.zDisplayedMessage),
            "Added account message is displayed");
   }

   @Test(description="Wrong password when creating Gmail Account", groups = { "functional" })
   public void wrongPasswordGmailAccount() throws HarnessException {

      String wrongPassword = ZimbraSeleniumProperties.getUniqueString();
      DesktopAccountItem desktopAccountItem = DesktopAccountItem.generateDesktopGmailAccountItem(
            AjaxCommonTest.gmailUserName, wrongPassword);

      FormAddGmailAccount accountForm = (FormAddGmailAccount)app.zPageAddNewAccount.zDropDownListSelect(
            DROP_DOWN_OPTION.GMAIL);
      accountForm.zFill(desktopAccountItem);
      accountForm.zSubmit();

      String message = app.zPageLogin.zGetMessage();
      ZAssert.assertStringContains(message,
            "User account authentication failed. Please check username and password.",
            "Verify error message of wrong password");

      app.zPageLogin.zNavigateTo();

      String welcomeMessage = app.zPageLogin.zGetWelcomeMessage();
      ZAssert.assertStringContains(welcomeMessage,
            "Zimbra Desktop allows you to access email while you are disconnected from the internet.",
            "Verify welcome message is displayed");

      ZAssert.assertEquals(false,
            app.zPageLogin.sIsElementPresent(PageLogin.Locators.zDisplayedMessage),
            "Added account message is displayed");
   }

   @Test(description="Wrong email address when creating Yahoo Account", groups = { "functional" })
   public void wrongEmailAddressYahooAccount() throws HarnessException {

      String wrongEmailAddress = ZimbraSeleniumProperties.getUniqueString() + "@yahoo.com";
      DesktopAccountItem desktopAccountItem = DesktopAccountItem.generateDesktopYahooAccountItem(
            wrongEmailAddress, AjaxCommonTest.yahooPassword);

      FormAddYahooAccount accountForm = (FormAddYahooAccount)app.zPageAddNewAccount.zDropDownListSelect(
            DROP_DOWN_OPTION.YAHOO);
      accountForm.zFill(desktopAccountItem);
      accountForm.zSubmit();

      String message = app.zPageLogin.zGetMessage();
      ZAssert.assertStringContains(message,
            "The requested user account does not exist. Please check the spelling.",
            "Verify error message of wrong password");

      app.zPageLogin.zNavigateTo();

      String welcomeMessage = app.zPageLogin.zGetWelcomeMessage();
      ZAssert.assertStringContains(welcomeMessage,
            "Zimbra Desktop allows you to access email while you are disconnected from the internet.",
            "Verify welcome message is displayed");

      ZAssert.assertEquals(false,
            app.zPageLogin.sIsElementPresent(PageLogin.Locators.zDisplayedMessage),
            "Added account message is displayed");
   }

   @Test(description="Wrong password when creating Yahoo Account", groups = { "functional" })
   public void wrongPasswordYahooAccount() throws HarnessException {

      String wrongPassword = ZimbraSeleniumProperties.getUniqueString();
      DesktopAccountItem desktopAccountItem = DesktopAccountItem.generateDesktopYahooAccountItem(
            AjaxCommonTest.yahooUserName, wrongPassword);

      FormAddYahooAccount accountForm = (FormAddYahooAccount)app.zPageAddNewAccount.zDropDownListSelect(
            DROP_DOWN_OPTION.YAHOO);
      accountForm.zFill(desktopAccountItem);
      accountForm.zSubmit();

      String message = app.zPageLogin.zGetMessage();
      ZAssert.assertStringContains(message,
            "Invalid password.",
            "Verify error message of wrong password");

      app.zPageLogin.zNavigateTo();

      String welcomeMessage = app.zPageLogin.zGetWelcomeMessage();
      ZAssert.assertStringContains(welcomeMessage,
            "Zimbra Desktop allows you to access email while you are disconnected from the internet.",
            "Verify welcome message is displayed");

      ZAssert.assertEquals(false,
            app.zPageLogin.sIsElementPresent(PageLogin.Locators.zDisplayedMessage),
            "Added account message is displayed");
   }

   @AfterMethod(alwaysRun=true)
   public void cleanUp() throws HarnessException {
      if (_sslIsModified) {
         Stafzmtlsctl stafzmtlsctl = new Stafzmtlsctl();
         stafzmtlsctl.setServerAccess(SERVER_ACCESS.HTTP);
      }

      ZimbraAccount.ResetAccountZWC();
      app.zPageLogin.zNavigateTo();
   }
}
