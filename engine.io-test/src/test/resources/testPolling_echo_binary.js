var engine = require('engine.io-client');
var helpers = require('./helpers');

var returnError = function (err) {
    process.exit(1);
};

var port = process.env.PORT || 3000;
socket = engine('http://127.0.0.1:' + port, {
    transports: ['polling']
});
socket.on('open', function () {
    var echoMessage = helpers.toArrayBuffer([1, 2, 3, 4, 5, 6, 7, 8]);
    socket.on('message', function (message) {
        message = helpers.toArrayBuffer(message);

        if(helpers.compareArrayBuffer(message, echoMessage)) {
            process.exit(0);
        } else {
            returnError();
        }
    });
    socket.send(echoMessage);
});
socket.on('error', returnError);
setTimeout(returnError, 750);