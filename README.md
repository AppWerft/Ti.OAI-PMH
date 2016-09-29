#Ti.OAI-PMH

<img src="http://www.openarchives.org/images/OA200.gif" align="left"/>Wiki: *The Open Archives Initiative Protocol for Metadata Harvesting (OAI-PMH) is a protocol developed for harvesting (or collecting) metadata descriptions of records in an archive so that services can be built using metadata from many archives. An implementation of OAI-PMH must support representing metadata in Dublin Core, but may also support additional representations.*

This module realise an Android module for Titanium. The http based prorotcol *OAI-PMH* is an alternative to the protocol [Z39.50](https://en.wikipedia.org/wiki/Z39.50). *OAI-PMH* is a HTTP based protocol and uses XML as trasnfer format. *Z39.50* is older as OAI-PMH and is sessions based. 

##Usage

```javascript
var OAIPMH = require("de.appwerft.oaipmh");
```

First we can retreive a list of all providers:
```javascript
OAIPMH.getList({
   filter : ".de",   // optional  
   function(e) {
      console.log(e.data);
   }
);
```


Now we can present a list for the user, the user select one provider and now we connect to it:
```javascript
var Provider = createProvider({
    timeout : 10000, // optional
    retries : 0, // optional
    url : "http://memory.loc.gov/cgi-bin/oai2_0"
});
```
After connecting with a provider you can ask all questions:

```javascript
Provider.Identify(
    null,  // dummy for clean code
    function(e) {
      console.log(e["OAI-PMH"]);
    },
    function(err) {
        console.log(err.message);
    },
});
```
This request give us [this answer](https://raw.githubusercontent.com/AppWerft/Ti.OAI-PMH/master/documentation/verb%3Didentify).

For getting the metadata formats:
```javascript
Provider.ListMetadataFormats(
    null,  // dummy for clean code
    function(e) {
        console.log(e["OAI-PMH"]);
    },
    function(err) {
        console.log(err.message);
    },
);

```

Now we can ask by filter:

```javascript
Provider.ListIdentifiers({
    from : "1998-01-15",
    until :  "1999-01-15",
    metadataPrefix : "oai_dc",
   },
   function(e) {
        console.log(e["OAI-PMH"]);
    },
    function(err) {
        console.log(err.message);
);
```
*listIdentifiers* returns a list if identifiers. Now you can ask:
```javascript
Provider.GetRecord({
        identifier : "oai:arXiv.org:hep-th/9901001",
        metadataPrefix : "oai_dc"},
    function(e) {
        console.log(e["OAI-PMH"]);
    },
    function(err) {
        console.log(err.message);
);
```

After all you can release by:
```javascript
Provider.close();
```
