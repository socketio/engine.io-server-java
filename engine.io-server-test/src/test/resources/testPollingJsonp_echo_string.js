var returnError = function () {
    window.setResult(1);
};

var port = arguments[0];
socket = eio('http://127.0.0.1:' + port, {
    transports: ['polling'],
    jsonp: true,
    forceJSONP: true
});
socket.on('open', function () {
    var echoMessage = "Hello World";
    socket.on('message', function (message) {
        if(message === echoMessage) {
            window.setResult(0);
        } else {
            returnError();
        }
    });
    socket.send(echoMessage);
});
socket.on('error', returnError);
setTimeout(returnError, 750);
