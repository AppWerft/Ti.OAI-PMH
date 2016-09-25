#Ti.OAI-PMH

<img src="http://www.openarchives.org/images/OA200.gif" align="left"/>Wiki: *The Open Archives Initiative Protocol for Metadata Harvesting (OAI-PMH) is a protocol developed for harvesting (or collecting) metadata descriptions of records in an archive so that services can be built using metadata from many archives. An implementation of OAI-PMH must support representing metadata in Dublin Core, but may also support additional representations.*

This module realise an Android module for Titanium. The http based prorotcol *OAI-PMH* is an alternative to the protocol [Z39.50](https://en.wikipedia.org/wiki/Z39.50). *OAI-PMH* is a HTTP based protocol and uses XML as trasnfer format. *Z39.50* is older as OAI-PMH and is sessions based. 

##Usage

```javascript
var OAIPMH = require("de.appwerft.oaipmh");
var Provider = createConnection("http://an.oa.org/");
```
After connecting with a provider you can ask all questions:

```javascript

Provider.identify({
   onload : function(e) {
      console.log(e.data);
   }
});
Provider.getRecord({
   identifier : "oai:arXiv.org:hep-th/9901001",
   metadataPrefix : "oai_dc",
   onload : function(e) {
      console.log(e.data);
   }
})
```
After all you can release by:
```javascript
Provider.close();
```
