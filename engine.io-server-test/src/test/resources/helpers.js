/* Helper methods */

module.exports = {};

module.exports.toArrayBuffer = function (buf) {
    var ab = new ArrayBuffer(buf.length);
    var view = new Uint8Array(ab);
    for (var i = 0; i < buf.length; ++i) {
        view[i] = buf[i];
    }
    return ab;
};

module.exports.compareArrayBuffer = function (arr1, arr2) {
    if(arr1.byteLength !== arr2.byteLength) {
        return false;
    }

    for (var i = 0; i < arr1.byteLength; i++) {
        if(arr1[i] !== arr2[i]) {
            return false;
        }
    }

    return true;
};