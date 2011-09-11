﻿using System.Collections.Generic;
using System.Xml.Linq;
using System.Linq;
using System.Text;
using System.Xml;
using System.IO;
using System.Reflection;

namespace CssLib
{
    public class ZimbraAPI
    {
        // Errors
        internal const int INCONSISTENT_LISTS    = 91;
        internal const int ACCOUNT_NO_NAME       = 98;
        internal const int ACCOUNT_CREATE_FAILED = 99;
        //

        // Upload modes
        public const int STRING_MODE = 1;           // for messages -- request is all string data
        public const int MIXED_MODE = 2;            // for contacts (or appts) -- mixed string and binary
        //

        // Values
        internal const int INLINE_LIMIT = 4000;     // smaller than this limit, we'll inline; larger, we'll upload  

        private string lastError;
        public string LastError
        {
            get { return lastError; }
            set
            {
                lastError = value;
            }
        }

        public ZimbraAPI()
        {
            ZimbraValues.GetZimbraValues();
        }

        // Parse Methods //////////////////
        // [note that we don't have Parse methods for CreateContact, CreateFolder, etc.]
        private string ParseSoapFault(string rsperr)
        {
            if (rsperr.Length == 0)
            {
                return "";
            }
            if (rsperr.IndexOf("<soap:Fault>") == -1)
            {
                return "";
            }
            string soapReason = "";

            XDocument xmlDoc = XDocument.Parse(rsperr);            
            XNamespace ns = "http://www.w3.org/2003/05/soap-envelope"; 
            IEnumerable<XElement> de =
                from el in xmlDoc.Descendants()
                    select el;
                    foreach (XElement el in de)
                        if (el.Name == ns + "Reason")
                        {
                            soapReason = el.Value;
                            break;
                        }  
            return soapReason;
        }

        private void ParseLogon(string rsp, bool isAdmin)
        {
            string authToken = "";
            string isDomainAdmin = "false";
            if (rsp != null)
            {
                int startIdx = rsp.IndexOf("<authToken>");
                if (startIdx != -1)
                {
                    XDocument xmlDoc = XDocument.Parse(rsp);
                    XNamespace ns = (isAdmin) ? "urn:zimbraAdmin" : "urn:zimbraAccount";

                    // we'll have to deal with this -- need to figure this out later -- with GetInfo
                    // for now, just faking -- always setting admin stuff to false if not admin -- not right
                    foreach (var objIns in xmlDoc.Descendants(ns + "AuthResponse"))
                    {
                        authToken += objIns.Element(ns + "authToken").Value;
                        isDomainAdmin = "false";
                        if (isAdmin)
                        {
                            var x = from a in objIns.Elements(ns + "a")
                                    where a.Attribute("n").Value == "zimbraIsDomainAdminAccount"
                                    select a.Value;
                            isDomainAdmin = x.ElementAt(0);
                        }
                    }
                }
            }
            ZimbraValues.GetZimbraValues().AuthToken = authToken;
            ZimbraValues.GetZimbraValues().IsAdminAccount = true;
            ZimbraValues.GetZimbraValues().IsDomainAdminAccount = (isDomainAdmin == "true");
        }

        private void ParseGetInfo(string rsp)
        {
            string accountName = "";
            string serverVersion = "";
            if (rsp != null)
            {
                int startNameIdx = rsp.IndexOf("<name>");
                int startVersionIdx = rsp.IndexOf("<version>");
                if ((startNameIdx != -1) && (startVersionIdx != -1))
                {
                    XDocument xmlDoc = XDocument.Parse(rsp);
                    XNamespace ns = "urn:zimbraAccount";
                    foreach (var objIns in xmlDoc.Descendants(ns + "GetInfoResponse"))
                    {
                        accountName += objIns.Element(ns + "name").Value;
                        serverVersion += objIns.Element(ns + "version").Value;
                    }
                }
            }
            ZimbraValues.GetZimbraValues().AccountName = accountName;
            ZimbraValues.GetZimbraValues().ServerVersion = serverVersion;
        }

        private int ParseGetAccount(string rsp)
        {
            int retval = 0;
            if (rsp != null)
            {
                int dIdx = rsp.IndexOf("account id=");
                if (dIdx != -1)
                {
                    XDocument xmlDoc = XDocument.Parse(rsp);
                    XNamespace ns = "urn:zimbraAdmin";
                    foreach (var objIns in xmlDoc.Descendants(ns + "GetAccountResponse"))
                    {
                        foreach (XElement accountIns in objIns.Elements())
                        {
                            foreach (XAttribute accountAttr in accountIns.Attributes())
                            {
                                if (accountAttr.Name == "name")
                                {
                                    retval = (accountAttr.Value).Length;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            return retval;
        }

        private int ParseCreateAccount(string rsp)
        {
            int retval = 0;
            if (rsp != null)
            {
                int dIdx = rsp.IndexOf("account id=");
                if (dIdx != -1)
                {
                    XDocument xmlDoc = XDocument.Parse(rsp);
                    XNamespace ns = "urn:zimbraAdmin";
                    foreach (var objIns in xmlDoc.Descendants(ns + "CreateAccountResponse"))
                    {
                        foreach (XElement accountIns in objIns.Elements())
                        {
                            foreach (XAttribute accountAttr in accountIns.Attributes())
                            {
                                if (accountAttr.Name == "name")
                                {
                                    retval = (accountAttr.Value).Length;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            return retval;
        }

        private void ParseGetAllDomain(string rsp)
        {
            if (rsp != null)
            {
                int dIdx = rsp.IndexOf("domain");
                if (dIdx != -1)
                {
                    XDocument xmlDoc = XDocument.Parse(rsp);
                    XNamespace ns = "urn:zimbraAdmin";
                    foreach (var objIns in xmlDoc.Descendants(ns + "GetAllDomainsResponse"))
                    {
                        foreach (XElement domainIns in objIns.Elements())
                        {
                            foreach (XAttribute domainAttr in domainIns.Attributes())
                            {
                                if (domainAttr.Name == "name")
                                {
                                    ZimbraValues.GetZimbraValues().Domains.Add(domainAttr.Value);
                                }
                            }
                        }
                    }
                }
            }
        }

        private void ParseGetAllCos(string rsp)
        {
            if (rsp != null)
            {
                int dIdx = rsp.IndexOf("cos");
                if (dIdx != -1)
                {
                    XDocument xmlDoc = XDocument.Parse(rsp);
                    XNamespace ns = "urn:zimbraAdmin";
                    foreach (var objIns in xmlDoc.Descendants(ns + "GetAllCosResponse"))
                    {
                        foreach (XElement cosIns in objIns.Elements())
                        {
                            string name = "";
                            string id = "";
                            foreach (XAttribute cosAttr in cosIns.Attributes())
                            {
                                if (cosAttr.Name == "name")
                                {
                                    name = cosAttr.Value;
                                }
                                if (cosAttr.Name == "id")
                                {
                                    id = cosAttr.Value;
                                }
                            }
                            if ((name.Length > 0) || (id.Length > 0))
                            {
                                ZimbraValues.GetZimbraValues().COSes.Add(new CosInfo(name, id));
                            }
                        }
                    }
                }
            }
        }

        // may not need this -- it's here anyway for now
        private void ParseAddMsg(string rsp, out string mID)
        {
            mID = "";
            if (rsp != null)
            {
                int midIdx = rsp.IndexOf("m id");
                if (midIdx != -1)
                {
                    XDocument xmlDoc = XDocument.Parse(rsp);
                    XNamespace ns = "urn:zimbraMail";
                    foreach (var objIns in xmlDoc.Descendants(ns + "AddMsgResponse"))
                    {
                        foreach (XElement mIns in objIns.Elements())
                        {
                            foreach (XAttribute mAttr in mIns.Attributes())
                            {
                                if (mAttr.Name == "id")
                                {
                                    mID = mAttr.Value;
                                }
                            }
                        }
                    }
                }
            }
        }
        //////////

        // private UploadFile method
        private int UploadFile(string filepath, int mode, out string uploadToken)
        {
            WebServiceClient client = new WebServiceClient
            {
                Url = "https://" + ZimbraValues.GetZimbraValues().HostName + ":" + ZimbraValues.GetZimbraValues().Port + "/service/upload?fmt=raw",
                WSServiceType = WebServiceClient.ServiceType.Traditional
            };

            int retval = 0;
            string rsp = "";
            uploadToken = "";

            client.InvokeUploadService(ZimbraValues.GetZimbraValues().AuthToken, filepath, mode, out rsp);
            retval = client.status;
            if (retval == 0)
            {
                int li = rsp.LastIndexOf(",");
                if (li != -1)
                {
                    // get the string with the upload token, which will have a leading ' and a trailing '\r\n -- so strip that stuff off
                    int uti = li + 1;   // upload token index
                    string tmp = rsp.Substring(uti, (rsp.Length - uti));
                    int lastsinglequoteidx = tmp.LastIndexOf("'");
                    uploadToken = tmp.Substring(1, (lastsinglequoteidx - 1));
                }
            }

            return retval;
        }
        //

        // private API helper methods

        // example: <name>foo</name>
        private void WriteNVPair(XmlWriter writer, string name, string value)
        {
            writer.WriteStartElement(name);
            writer.WriteValue(value);
            writer.WriteEndElement();
        }

        // example: <a n="displayName">bar</a>
        private void WriteAttrNVPair(XmlWriter writer, string fieldType, string fieldName, string attrName, string attrValue)
        {
            writer.WriteStartElement(fieldType);
            writer.WriteStartAttribute(fieldName);
            writer.WriteString(attrName);
            writer.WriteEndAttribute();
            writer.WriteValue(attrValue);
            writer.WriteEndElement();
        }

        // example: <account by="name">foo@bar.com</account>
        private void WriteAccountBy(XmlWriter writer, string val)
        {
            WriteAttrNVPair(writer, "account", "by", "name", val);
        }

        private void WriteHeader(XmlWriter writer, bool bWriteSessionId, bool bWriteAuthtoken, bool bWriteAccountBy)
        {
            writer.WriteStartElement("Header", "http://www.w3.org/2003/05/soap-envelope");
            writer.WriteStartElement("context", "urn:zimbra");
            writer.WriteStartElement("nonotify");
            writer.WriteEndElement();   // nonotify
            writer.WriteStartElement("noqualify");
            writer.WriteEndElement();   // noqualify
            writer.WriteStartElement("nosession");
            writer.WriteEndElement();   // nosession
            if (bWriteSessionId)
            {
                writer.WriteStartElement("sessionId");
                writer.WriteEndElement();   // sessionId
            }
            if (bWriteAuthtoken)
            {
                WriteNVPair(writer, "authToken", ZimbraValues.zimbraValues.AuthToken);
            }
            if (bWriteAccountBy)    // would only happen after a logon
            {
                WriteAccountBy(writer, ZimbraValues.zimbraValues.AccountName);
            }
            writer.WriteEndElement();   // context
            writer.WriteEndElement();   // header
        }
        //

        // API methods /////////
        public int Logon(string hostname, string port, string username, string password, bool isAdmin)
        {
            lastError = "";
            string urn = "";

            if (isAdmin)
            {
                ZimbraValues.GetZimbraValues().Url = "https://" + hostname + ":" + port + "/service/admin/soap";
                urn = "urn:zimbraAdmin";
            }
            else
            {
                ZimbraValues.GetZimbraValues().Url = "http://" + hostname + ":" + port + "/service/soap";
                urn = "urn:zimbraAccount"; 
            }

            WebServiceClient client = new WebServiceClient
            {
                Url = ZimbraValues.GetZimbraValues().Url,
                WSServiceType = WebServiceClient.ServiceType.Traditional
            };

            StringBuilder sb = new StringBuilder();
            XmlWriterSettings settings = new XmlWriterSettings();
            settings.OmitXmlDeclaration = true;
            using (XmlWriter writer = XmlWriter.Create(sb, settings))
            {
                writer.WriteStartDocument();
                writer.WriteStartElement("soap", "Envelope", "http://www.w3.org/2003/05/soap-envelope");

                WriteHeader(writer, false, false, false);

                // body
                writer.WriteStartElement("Body", "http://www.w3.org/2003/05/soap-envelope");
                writer.WriteStartElement("AuthRequest", urn);

                if (isAdmin)
                {
                    WriteNVPair(writer, "name", username);
                }
                else
                {
                    WriteAccountBy(writer, username);
                }

                WriteNVPair(writer, "password", password);

                writer.WriteEndElement();   // AuthRequest
                writer.WriteEndElement();   // soap body
                // end body

                writer.WriteEndElement();   // soap envelope
                writer.WriteEndDocument();
            }

            string rsp = "";
            client.InvokeService(sb.ToString(), out rsp);
            if (client.status == 0)
            {
                ParseLogon(rsp, isAdmin);
                ZimbraValues.GetZimbraValues().HostName = hostname;
                ZimbraValues.GetZimbraValues().Port = port;
            }
            else
            {
                string soapReason = ParseSoapFault(client.errResponseMessage);
                if (soapReason.Length > 0)
                {
                    lastError = soapReason;
                }
                else
                {
                    lastError = client.exceptionMessage;
                }
            }
            return client.status;

        }

        public int GetInfo()
        {
            lastError = "";
            WebServiceClient client = new WebServiceClient
            {
                Url = ZimbraValues.GetZimbraValues().Url,
                WSServiceType = WebServiceClient.ServiceType.Traditional
            };

            StringBuilder sb = new StringBuilder();
            XmlWriterSettings settings = new XmlWriterSettings();
            settings.OmitXmlDeclaration = true;
            using (XmlWriter writer = XmlWriter.Create(sb, settings))
            {
                writer.WriteStartDocument();
                writer.WriteStartElement("soap", "Envelope", "http://www.w3.org/2003/05/soap-envelope");

                WriteHeader(writer, true, true, false);

                // body
                writer.WriteStartElement("Body", "http://www.w3.org/2003/05/soap-envelope");
                writer.WriteStartElement("GetInfoRequest", "urn:zimbraAccount");
                writer.WriteAttributeString("sections", "mbox");
                writer.WriteEndElement();   // GetInfoRequest
                writer.WriteEndElement();   // soap body
                // end body

                writer.WriteEndElement();   // soap envelope
                writer.WriteEndDocument();
            }

            string rsp = "";
            client.InvokeService(sb.ToString(), out rsp);
            if (client.status == 0)
            {
                ParseGetInfo(rsp);
            }
            else
            {
                string soapReason = ParseSoapFault(client.errResponseMessage);
                if (soapReason.Length > 0)
                {
                    lastError = soapReason;
                }
                else
                {
                    lastError = client.exceptionMessage;
                }
            }
            return client.status;
        }

        public int GetAllDomains()
        {
            lastError = "";
            WebServiceClient client = new WebServiceClient
            {
                Url = ZimbraValues.GetZimbraValues().Url,
                WSServiceType = WebServiceClient.ServiceType.Traditional
            };

            StringBuilder sb = new StringBuilder();
            XmlWriterSettings settings = new XmlWriterSettings();
            settings.OmitXmlDeclaration = true;
            using (XmlWriter writer = XmlWriter.Create(sb, settings))
            {
                writer.WriteStartDocument();
                writer.WriteStartElement("soap", "Envelope", "http://www.w3.org/2003/05/soap-envelope");

                WriteHeader(writer, true, true, false);

                // body
                writer.WriteStartElement("Body", "http://www.w3.org/2003/05/soap-envelope");
                writer.WriteStartElement("GetAllDomainsRequest", "urn:zimbraAdmin");
                writer.WriteEndElement();   // GetAllDomainsRequest
                writer.WriteEndElement();   // soap body
                // end body

                writer.WriteEndElement();   // soap envelope
                writer.WriteEndDocument();
            }

            string rsp = "";
            client.InvokeService(sb.ToString(), out rsp);
            if (client.status == 0)
            {
                ParseGetAllDomain(rsp);
            }
            else
            {
                string soapReason = ParseSoapFault(client.errResponseMessage);
                if (soapReason.Length > 0)
                {
                    lastError = soapReason;
                }
                else
                {
                    lastError = client.exceptionMessage;
                }
            }
            return client.status;
        }

        public int GetAllCos()
        {
            lastError = "";
            WebServiceClient client = new WebServiceClient
            {
                Url = ZimbraValues.GetZimbraValues().Url,
                WSServiceType = WebServiceClient.ServiceType.Traditional
            };

            StringBuilder sb = new StringBuilder();
            XmlWriterSettings settings = new XmlWriterSettings();
            settings.OmitXmlDeclaration = true;
            using (XmlWriter writer = XmlWriter.Create(sb, settings))
            {
                writer.WriteStartDocument();
                writer.WriteStartElement("soap", "Envelope", "http://www.w3.org/2003/05/soap-envelope");

                WriteHeader(writer, true, true, false);

                // body
                writer.WriteStartElement("Body", "http://www.w3.org/2003/05/soap-envelope");
                writer.WriteStartElement("GetAllCosRequest", "urn:zimbraAdmin");
                writer.WriteEndElement();   // GetAllCosRequest
                writer.WriteEndElement();   // soap body
                // end body

                writer.WriteEndElement();   // soap envelope
                writer.WriteEndDocument();
            }

            string rsp = "";
            client.InvokeService(sb.ToString(), out rsp);
            if (client.status == 0)
            {
                ParseGetAllCos(rsp);
            }
            else
            {
                string soapReason = ParseSoapFault(client.errResponseMessage);
                if (soapReason.Length > 0)
                {
                    lastError = soapReason;
                }
                else
                {
                    lastError = client.exceptionMessage;
                }
            }
            return client.status;
        }

        public int GetAccount(string accountname)
        {
            int retval = 0;
            lastError = "";
            WebServiceClient client = new WebServiceClient
            {
                Url = ZimbraValues.GetZimbraValues().Url,
                WSServiceType = WebServiceClient.ServiceType.Traditional
            };

            StringBuilder sb = new StringBuilder();
            XmlWriterSettings settings = new XmlWriterSettings();
            settings.OmitXmlDeclaration = true;
            using (XmlWriter writer = XmlWriter.Create(sb, settings))
            {
                writer.WriteStartDocument();
                writer.WriteStartElement("soap", "Envelope", "http://www.w3.org/2003/05/soap-envelope");

                WriteHeader(writer, true, true, false);

                // body
                writer.WriteStartElement("Body", "http://www.w3.org/2003/05/soap-envelope");
                writer.WriteStartElement("GetAccountRequest", "urn:zimbraAdmin");

                WriteAccountBy(writer, accountname);

                writer.WriteEndElement();   // GetAccountRequest
                writer.WriteEndElement();   // soap body
                // end body

                writer.WriteEndElement();   // soap envelope
                writer.WriteEndDocument();
            }

            string rsp = "";
            client.InvokeService(sb.ToString(), out rsp);
            retval = client.status;
            if (client.status == 0)
            {
                if (ParseGetAccount(rsp) == 0)  // length of name is 0 -- this is bad
                {
                    retval =  ACCOUNT_NO_NAME;
                }
            }
            else
            {
                string soapReason = ParseSoapFault(client.errResponseMessage);
                if (soapReason.Length > 0)
                {
                    lastError = soapReason;
                }
                else
                {
                    lastError = client.exceptionMessage;
                }
            }
            return retval;
        }

        public int CreateAccount(string accountname, string defaultpw, string cosid)
        {
            int retval = 0;
            lastError = "";
            string displayname = accountname.Substring(0, accountname.IndexOf("@"));
            string zimbraForeignPrincipal = "ad:" + displayname;

            WebServiceClient client = new WebServiceClient
            {
                Url = ZimbraValues.GetZimbraValues().Url,
                WSServiceType = WebServiceClient.ServiceType.Traditional
            };

            StringBuilder sb = new StringBuilder();
            XmlWriterSettings settings = new XmlWriterSettings();
            settings.OmitXmlDeclaration = true;
            using (XmlWriter writer = XmlWriter.Create(sb, settings))
            {
                writer.WriteStartDocument();
                writer.WriteStartElement("soap", "Envelope", "http://www.w3.org/2003/05/soap-envelope");

                WriteHeader(writer, true, true, false);

                // body
                writer.WriteStartElement("Body", "http://www.w3.org/2003/05/soap-envelope");
                writer.WriteStartElement("CreateAccountRequest", "urn:zimbraAdmin");

                WriteNVPair(writer, "name", accountname);
                WriteNVPair(writer, "password", defaultpw);

                WriteAttrNVPair(writer, "a", "n", "displayName", displayname);
                WriteAttrNVPair(writer, "a", "n", "zimbraForeignPrincipal", zimbraForeignPrincipal);
                WriteAttrNVPair(writer, "a", "n", "zimbraCOSId", cosid);

                writer.WriteEndElement();   // CreateAccountRequest
                writer.WriteEndElement();   // soap body
                // end body

                writer.WriteEndElement();   // soap envelope
                writer.WriteEndDocument();
            }

            string rsp = "";
            client.InvokeService(sb.ToString(), out rsp);
            retval = client.status;
            if (client.status == 0)
            {
                if (ParseCreateAccount(rsp) == 0)  // length of name is 0 -- this is bad
                {
                    retval = ACCOUNT_CREATE_FAILED;
                }
            }
            else
            {
                string soapReason = ParseSoapFault(client.errResponseMessage);
                if (soapReason.Length > 0)
                {
                    lastError = soapReason;
                }
                else
                {
                    lastError = client.exceptionMessage;
                }
            }
            return retval;
        }

        public void CreateContactRequest(XmlWriter writer, Dictionary<string, string> contact, int requestId)
        {
            writer.WriteStartElement("CreateContactRequest", "urn:zimbraMail");
            if (requestId != -1)
            {
                writer.WriteAttributeString("requestId", requestId.ToString());
            }
            writer.WriteStartElement("cn");
            writer.WriteAttributeString("l", "7");
            
            foreach (KeyValuePair<string, string> pair in contact)
            {
                string nam = pair.Key;
                string val = pair.Value;

                if (nam == "image")
                {
                    string uploadToken = "";
                    if (UploadFile(val, MIXED_MODE, out uploadToken) == 0)
                    {
                        writer.WriteStartElement("a");
                        writer.WriteAttributeString("n", nam);
                        writer.WriteAttributeString("aid", uploadToken);
                        writer.WriteEndElement();
                    }
                }
                else
                {
                    WriteAttrNVPair(writer, "a", "n", nam, val);
                }
            }
            
            writer.WriteEndElement();   // cn
            writer.WriteEndElement();   // CreateContactRequest
        }

        public int CreateContact(Dictionary<string, string> contact)
        {
            lastError = "";
            WebServiceClient client = new WebServiceClient
            {
                Url = ZimbraValues.GetZimbraValues().Url,
                WSServiceType = WebServiceClient.ServiceType.Traditional
            };

            int retval = 0;

            StringBuilder sb = new StringBuilder();
            XmlWriterSettings settings = new XmlWriterSettings();
            settings.OmitXmlDeclaration = true;
            using (XmlWriter writer = XmlWriter.Create(sb, settings))
            {
                writer.WriteStartDocument();
                writer.WriteStartElement("soap", "Envelope", "http://www.w3.org/2003/05/soap-envelope");

                WriteHeader(writer, true, true, true);

                writer.WriteStartElement("Body", "http://www.w3.org/2003/05/soap-envelope");

                CreateContactRequest(writer, contact, -1);

                writer.WriteEndElement();   // soap body
                writer.WriteEndElement();   // soap envelope
                writer.WriteEndDocument();
            }

            string rsp = "";
            client.InvokeService(sb.ToString(), out rsp);
            retval = client.status;
            return retval;
        }

        public int CreateContacts(List<Dictionary<string, string>> lContacts)
        {
            lastError = "";
            WebServiceClient client = new WebServiceClient
            {
                Url = ZimbraValues.GetZimbraValues().Url,
                WSServiceType = WebServiceClient.ServiceType.Traditional
            };

            int retval = 0;

            StringBuilder sb = new StringBuilder();
            XmlWriterSettings settings = new XmlWriterSettings();
            settings.OmitXmlDeclaration = true;
            using (XmlWriter writer = XmlWriter.Create(sb, settings))
            {
                writer.WriteStartDocument();
                writer.WriteStartElement("soap", "Envelope", "http://www.w3.org/2003/05/soap-envelope");

                WriteHeader(writer, true, true, true);

                writer.WriteStartElement("Body", "http://www.w3.org/2003/05/soap-envelope");
                writer.WriteStartElement("BatchRequest", "urn:zimbra");

                for (int i = 0; i < lContacts.Count; i++)
                {
                    Dictionary<string, string> contact = lContacts[i];
                    CreateContactRequest(writer, contact, i);
                }

                writer.WriteEndElement();   // BatchRequest
                writer.WriteEndElement();   // soap body
                writer.WriteEndElement();   // soap envelope
                writer.WriteEndDocument();
            }

            string rsp = "";
            client.InvokeService(sb.ToString(), out rsp);
            retval = client.status;
            return retval;
        }

        public void AddMsgRequest(XmlWriter writer, string uploadInfo, ZimbraMessage message, bool isInline, int requestId)
        {
            // if isLine, uploadInfo will be a file path; if not, uploadInfo will be the upload token
            writer.WriteStartElement("AddMsgRequest", "urn:zimbraMail");
            if (requestId != -1)
            {
                writer.WriteAttributeString("requestId", requestId.ToString());
            }
            writer.WriteStartElement("m");
            writer.WriteAttributeString("l", message.folderId);
            writer.WriteAttributeString("d", message.rcvdDate);
            writer.WriteAttributeString("f", message.flags);
            if (isInline)
            {
                WriteNVPair(writer, "content", System.Text.Encoding.Default.GetString(File.ReadAllBytes(uploadInfo)));
                
            }
            else
            {
                writer.WriteAttributeString("aid", uploadInfo);
            }

            writer.WriteEndElement();   // m
            writer.WriteEndElement();   // AddMsgRequest
        }

        public int AddMessage(string filepath, ZimbraMessage message)
        {
            lastError = "";
            string uploadInfo = "";
            int retval = 0;

            FileInfo f = new FileInfo(filepath);
            bool isInline = (f.Length < INLINE_LIMIT);
            if (isInline)
            {
                uploadInfo = filepath;
            }
            else
            {
                retval = UploadFile(filepath, STRING_MODE, out uploadInfo);
            }

            if (retval == 0)
            {
                WebServiceClient client = new WebServiceClient
                {
                    Url = ZimbraValues.GetZimbraValues().Url,
                    WSServiceType = WebServiceClient.ServiceType.Traditional
                };

                StringBuilder sb = new StringBuilder();
                XmlWriterSettings settings = new XmlWriterSettings();
                settings.OmitXmlDeclaration = true;
                using (XmlWriter writer = XmlWriter.Create(sb, settings))
                {
                    writer.WriteStartDocument();
                    writer.WriteStartElement("soap", "Envelope", "http://www.w3.org/2003/05/soap-envelope");

                    WriteHeader(writer, true, true, true);

                    writer.WriteStartElement("Body", "http://www.w3.org/2003/05/soap-envelope");

                    AddMsgRequest(writer, uploadInfo, message, isInline, -1);

                    writer.WriteEndElement();   // soap body
                    writer.WriteEndElement();   // soap envelope
                    writer.WriteEndDocument();
                }

                string rsp = "";
                client.InvokeService(sb.ToString(), out rsp);
                retval = client.status;
                if (client.status == 0)
                {
                    string mID = "";
                    ParseAddMsg(rsp, out mID);  // get the id
                }
                else
                {
                    string soapReason = ParseSoapFault(client.errResponseMessage);
                    if (soapReason.Length > 0)
                    {
                        lastError = soapReason;
                    }
                    else
                    {
                        lastError = client.exceptionMessage;
                    }
                }
            }

            return retval;
        }

        public int AddMessages(List<string> lFilePaths, List<ZimbraMessage> lMessages)
        {
            if (lFilePaths.Count != lMessages.Count)
            {
                return INCONSISTENT_LISTS;
            }

            int retval = 0;
            int ufretval = 0;
            lastError = "";
            string uploadInfo = "";

            WebServiceClient client = new WebServiceClient
            {
                Url = ZimbraValues.GetZimbraValues().Url,
                WSServiceType = WebServiceClient.ServiceType.Traditional
            };

            StringBuilder sb = new StringBuilder();
            XmlWriterSettings settings = new XmlWriterSettings();
            settings.OmitXmlDeclaration = true;
            using (XmlWriter writer = XmlWriter.Create(sb, settings))
            {
                writer.WriteStartDocument();
                writer.WriteStartElement("soap", "Envelope", "http://www.w3.org/2003/05/soap-envelope");

                WriteHeader(writer, true, true, true);

                writer.WriteStartElement("Body", "http://www.w3.org/2003/05/soap-envelope");
                writer.WriteStartElement("BatchRequest", "urn:zimbra");

                for (int i = 0; i < lFilePaths.Count; i++)
                {
                    FileInfo f = new FileInfo(lFilePaths[i]);
                    bool isInline = (f.Length < INLINE_LIMIT);
                    if (isInline)
                    {
                        uploadInfo = lFilePaths[i];
                    }
                    else
                    {
                        ufretval = UploadFile(lFilePaths[i], STRING_MODE, out uploadInfo);
                    }
                    
                    if (ufretval == 0)
                    {
                        ZimbraMessage message = lMessages[i];
                        AddMsgRequest(writer, uploadInfo, message, isInline, -1);
                    }
                }

                writer.WriteEndElement();   // BatchRequest
                writer.WriteEndElement();   // soap body
                writer.WriteEndElement();   // soap envelope
                writer.WriteEndDocument();
            }

            string rsp = "";
            client.InvokeService(sb.ToString(), out rsp);
            retval = client.status;

            return retval;
        }

        private void CreateFolderRequest(XmlWriter writer, ZimbraFolder folder, int requestId)
        {
            writer.WriteStartElement("CreateFolderRequest", "urn:zimbraMail");
            if (requestId != -1)
            {
                writer.WriteAttributeString("requestId", requestId.ToString());
            }
            writer.WriteStartElement("folder");
            writer.WriteAttributeString("name", folder.name);
            writer.WriteAttributeString("l", folder.parent);
            if (folder.view.Length > 0)
            {
                writer.WriteAttributeString("view", folder.view);
            }
            if (folder.color.Length > 0)
            {
                writer.WriteAttributeString("color", folder.color);
            }
            if (folder.flags.Length > 0)
            {
                writer.WriteAttributeString("f", folder.flags);
            }

            writer.WriteEndElement();   // folder
            writer.WriteEndElement();   // CreateFolderRequest
        }

        private int DoCreateFolder(ZimbraFolder folder)
        {
            lastError = "";
            WebServiceClient client = new WebServiceClient
            {
                Url = ZimbraValues.GetZimbraValues().Url,
                WSServiceType = WebServiceClient.ServiceType.Traditional
            };

            int retval = 0;

            StringBuilder sb = new StringBuilder();
            XmlWriterSettings settings = new XmlWriterSettings();
            settings.OmitXmlDeclaration = true;
            using (XmlWriter writer = XmlWriter.Create(sb, settings))
            {
                writer.WriteStartDocument();
                writer.WriteStartElement("soap", "Envelope", "http://www.w3.org/2003/05/soap-envelope");

                WriteHeader(writer, true, true, true);

                writer.WriteStartElement("Body", "http://www.w3.org/2003/05/soap-envelope");

                CreateFolderRequest(writer, folder, -1);

                writer.WriteEndElement();   // soap body
                writer.WriteEndElement();   // soap envelope
                writer.WriteEndDocument();
            }

            string rsp = "";
            client.InvokeService(sb.ToString(), out rsp);
            retval = client.status;
            return retval;
        }

        public int CreateFolder(string Name, string Parent, string View, string Color, string Flags)
        {
            return DoCreateFolder(new ZimbraFolder(Name, Parent, View, Color, Flags));
        }

        public int CreateFolder(string Name, string Parent)
        {
            return DoCreateFolder(new ZimbraFolder(Name, Parent, "", "", ""));
        }

        /////////////////////////

    }
}

