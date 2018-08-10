var engine = require('engine.io-client');
var helpers = require('./helpers');

var returnError = function (err) {
    process.exit(1);
};

var port = process.env.PORT || 3000;
socket = engine('http://127.0.0.1:' + port, {
    transports: ['websocket']
});
socket.on("message", function (message) {
    socket.sendPacket("message", message);

    setTimeout(function () {
        process.exit(0);
    }, 100);
});
socket.on('error', returnError);
setTimeout(returnError, 1500);