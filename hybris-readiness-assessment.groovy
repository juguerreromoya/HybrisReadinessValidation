import de.hybris.platform.servicelayer.config.ConfigurationService;
import java.text.SimpleDateFormat;
import java.util.Date;


def configurationService = spring.getBean("configurationService");

/** Set your email Recipient list **/
def emailRecipientList = ['info@hybrisarchitect.com', 'mraygoza@hybrisarchitect.com'];
                       

file = File.createTempFile("hybris-readiness-report",".tmp")
file.deleteOnExit()
pw = new PrintWriter(file)
def headers = ['Validation Type','Property Name', 'Property Value','Expected Value', 'Pass/Fail','Comment']
pw.println  headers.join(",");


/* Set the Tomcat validations and its appropriate values **/
def tomcatValidations = [
        "system.unlocking.disabled"  : "false",
        "tomcat.maxthreads":"200",
        "tomcat.development.mode":"false",
        "db.pool.maxActive":"90"
]


tomcatValidations.each { entry ->
    pw.println evaluate("Tomcat",entry.key,entry.value);
}

/* Set the Config validations and its appropriate values **/
def configValidations = [
        "servicelayer.prefetch"  : "default",
        "regioncache.entityregion.size"  : "100000",
        "regioncache.querycacheregion.size"  : "20000",
        "regioncache.productregion.size"  : "40000",
        "regioncache.productregion.evictionpolicy"  : "LRU",
        "regioncache.productregion.exclusiveComputation"  : "false",
        "regioncache.productregion.statsEnabled"  : "true",
        "clustermode":"false",
        "task.engine.loadonstartup": "true",
        "storefront.resourceBundle.cacheSeconds": "1",
        "storefront.show.debug.info": "true",
        "build.development.mode": "true",
        "mediafilter.response.header.Cache-Control":"public,max-age=31536000"
         
]


configValidations.each { entry ->
    pw.println evaluate("Configuration",entry.key,entry.value);
}

/* Set the Security validations and its appropriate values **/
def securityValidations = [
        "system.unlocking.disabled"  : "false",
        "productcockpit.default.login" : "",
		"productcockpit.default.password" :	 "",	
		"cmscockpit.default.login" : "",
		"cmscockpit.default.password" :	"",
		"cscockpit.default.login" :"",
		"cscockpit.default.password" : "",
		"hmc.default.login" : "",
		"hmc.default.password" : "",
		"login.anonymous.always.disabled" : "true"
        
]


securityValidations.each { entry ->
    pw.println evaluate("Security",entry.key,entry.value);
}


pw.close()

 /* Send email **/
 email = de.hybris.platform.util.mail.MailUtils.getPreConfiguredEmail()
 
 for(emailRecipient in emailRecipientList){
    email.addTo(emailRecipient); 
}
  
    
    Date dateNow =  new Date();
    
    String formatedDate = new SimpleDateFormat("yyyy-MM-dd").format(dateNow); 


    email.subject = 'SAP Hybris Readiness Report ' + formatedDate;
    email.msg = 'Hi Team, Attached is the SAP Hybris Readiness Report for ' + formatedDate;
   
   def reportName = 'SAP_Hybris_Readiness_Report_'+formatedDate+'.csv';
   // Create an attachment that is our temporary file
   attachment             = new org.apache.commons.mail.EmailAttachment();
   attachment.path        = file.absolutePath
   attachment.disposition = org.apache.commons.mail.EmailAttachment.ATTACHMENT
   attachment.description = reportName
   attachment.name        = reportName

   // Attach the attachment
   email.attach(attachment)

   // Send email
   email.send()

   // Clean the temp file
   file.delete()


def evaluate(testType,propName,expectedValue) {
  
  def propValue 	= configurationService.getConfiguration().getProperty(propName);
  def passedTest 	=  (propValue == expectedValue) ? "Pass" : "Fail";
  def comment    	= getComment(propName);
  def row  			= [testType,propName,propValue,expectedValue,passedTest,comment];
  return  row.join(",") ;
  
}


def getComment(propName) {
   
  switch (propName) {
         case "system.unlocking.disabled": 
           return "Lock the initialization button in HAC and then prevent unlocking by setting this property to true. This will prevent users from inadvertently initializing or updating your system.";
         case "tomcat.maxthreads":
    	    return "This is based on assumptions: a typical B2C Accelerator workloads on a 4 core storefront node. The naïve sizing recommendation is ~50 (concurrent) page views per second per core. Use this as a starting point.";
         case "tomcat.development.mode":
            return "Every X seconds (modificationTestInterval parameter), JSPs will be checked for modification. This parameter should be changed to false except in local development sandboxes.";
         case "db.pool.maxActive":
    		return "Value should rarely be changed. Reduce to avoid database saturation on undersized environments. This is a per-server tunable, and the database must handle this number of concurrent queries multiplied by the active number of nodes in the cluster. In the case of a four node cluster, this means 360 concurrent database queries. Tune in close partnership with skilled DBAs.";
    	 case "servicelayer.prefetch":
    	 	return "Alter the lazy list loading strategy for member variables of models. In Hybris Commerce 4, the default was literal and often a performance issue. in Hybris Commerce 5 the default is none. none: no attribute is automatically loaded but will be lazy-loaded the first time it is being accessed. literal: only literal values are loaded (or collection of literal values), references are lazy-loaded upon first access (this is default if parameter is missing). all: all attributes are loaded into the member variables (slow)";
    	 case "regioncache.entityregion.size":	
    	 	return "We recommend setting this up to 500K if you have >5G available. Profile cached item sizes carefully.";
    	 case "regioncache.querycacheregion.size":
    	 	return "Monitor the query cache size and hit count percentage to decide if this needs to be increased. Profile database query results carefully"; 	
		 case "clustermode":
		 	return "Set clustermode to true for production cluster based environments. For standalone/sandbox (development and testing) environments set to false.";
        case "task.engine.loadonstartup": 
        	return "Enables task processing, 'true' by default, setting it to 'false' disables this feature, meaning tasks and cronjobs will not be executed on the current node or server. Use this on the storefront and dedicated backoffice nodes with cluster node auto discovery to control cron job impact.";
        case "storefront.resourceBundle.cacheSeconds": 
        	return "Cache for property files. In production environments, set to -1 (never expire)";
        case "storefront.show.debug.info": 
        	return "Set to false to improve performance on performance environments";
        case "build.development.mode": 
        	return "In build/development mode make sure this property is set to true for all -items.xml files. Set this property to false for production environments."
        case "mediafilter.response.header.Cache-Control":
        	return "For production environments set the max-age to the following set this property to the following value:public,max-age=31536000";
        case "productcockpit.default.login": 
        	return "The default value is productmanager for the productcockpit, which should be removed by setting this value to an empty string ('')";
		case "productcockpit.default.password":
			return "The default value is 1234 for the productcockpit, which should be removed by setting this value to an empty string ('')";
		case "cmscockpit.default.login":
			return "The default value is cmsmanager for the cmscockpit, which should be removed by setting this value to an empty string ('')";
		case "cmscockpit.default.password":
			return "The default value is 1234 for the cmscockpit, which should be removed by setting this value to an empty string ('')";
		case "cscockpit.default.login":
			return "The default value is cmsmanager for the cscockpit, which should be removed by setting this value to an empty string ('')";
		case "cscockpit.default.password":
			return "The default value is 1234 for the cscockpit, which should be removed by setting this value to an empty string ('')";
		case "hmc.default.login":
			return "The default value is admin for the hmc, which should be removed by setting this value to an empty string ('')";
		case "hmc.default.password":
			return "The default value is nimda for the hmc, which should be removed by setting this value to an empty string ('')";
		case "login.anonymous.always.disabled":
			return "This should be set to true for all environments.";
		 default:
    	 	return "";	
  }
  
}
