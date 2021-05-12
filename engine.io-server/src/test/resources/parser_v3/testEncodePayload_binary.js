var helpers = require('../helpers');
var getStdin = require('get-stdin');
var parser = require('engine.io-parser');

var stdout = process.stdout;

getStdin.buffer().then(function (stdin) {
    var inputMessages = JSON.parse(stdin);
    var packets = [];
    for (var i = 0; i < inputMessages.length; i++) {
        packets.push({
            type: 'message',
            data: typeof inputMessages[i] === 'string' ? inputMessages[i] : helpers.toArrayBuffer(inputMessages[i])
        });
    }

    parser.encodePayload(packets, true, function (encodedValue) {
        stdout.write(encodedValue);
    });
});
