var helpers = require('../helpers');
var getStdin = require('get-stdin');
var parser = require('engine.io-parser');

var stdout = process.stdout;

getStdin.buffer().then(function (stdin) {
    stdin = helpers.toArrayBuffer(stdin);
    parser.encodePacket({ type: 'message', data: stdin }, false, function (encodedValue) {
        stdout.write(encodedValue);
    });
});
