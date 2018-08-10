var engine = require('engine.io-client');

var port = process.env.PORT || 3000;
socket = engine('http://127.0.0.1:' + port, {
    transports: ['polling']
});
socket.on('open', function () {
    process.exit(0);
});
socket.on('error', function () {
    process.exit(1);
});