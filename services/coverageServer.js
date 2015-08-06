#!/usr/bin/env node

process.on('uncaughtException', function (exception) {
   // handle or ignore error
});

var minion = require('../minion'),
    http = require('http'),
    app = minion(),
    server = http.createServer(app),
    BinaryServer = require('binaryjs').BinaryServer,
    port = 8047;
    
// process command line options
process.argv.forEach(function (val, index, array) {
  if(val == '--port' && array[index+1]) port = array[index+1];
});

// setup socket
var bs = BinaryServer({server: server});

// start server
server.listen(port);

// define tool
var tool = {
   apiVersion : "0.1",
   name : 'coverage',
   path: 'coverage.sh', 
   // instructional data used in /help
   description : 'coverage service',
   exampleUrl : ""
};

// add tool to minion server
minion.addTool(tool);

// start minion socket
minion.listen(bs);
console.log('coverageServer started on port ' + port);