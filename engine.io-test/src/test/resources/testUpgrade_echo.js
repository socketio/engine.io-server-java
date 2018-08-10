var engine = require('engine.io-client');

var returnError = function () {
    process.exit(1);
};

var port = process.env.PORT || 3000;
socket = engine('http://127.0.0.1:' + port, {});
socket.on('upgrade', function () {
    var echoMessage = "Upgraded Hello World";
    socket.on('message', function (message) {
        if(message === echoMessage) {
            process.exit(0);
        } else {
            returnError();
        }
    });
    socket.send(echoMessage);
});
socket.on('error', returnError);
setTimeout(returnError, 1000);