var helpers = require('../helpers');
var getStdin = require('get-stdin');
var parser = require('engine.io-parser');

var stdout = process.stdout;

getStdin().then(function (stdin) {
    parser.encodePacket({ type: 'message', data: stdin }, false, function (encodedValue) {
        stdout.write(encodedValue);
    });
});
