var returnError = function () {
    window.setResult(1);
};

var port = arguments[0];
socket = eio('http://127.0.0.1:' + port, {
    transports: ['polling'],
    jsonp: true,
    forceJSONP: true
});
socket.on("message", function (message) {
    socket.sendPacket("message", message);

    setTimeout(function () {
        window.setResult(0);
    }, 100);
});
socket.on('error', returnError);
setTimeout(returnError, 750);