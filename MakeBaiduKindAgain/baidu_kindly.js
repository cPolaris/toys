function is_normal_result(node) {
    console.log(typeof(node));
    return false;
}

console.log('executing content_script');

// Remove content_right which arrogantly displays some related searches
var right = document.getElementById('content_right');
right.parentNode.removeChild(right);

// For content_left, sanitize anything other than those divs with class name containing 'result'
var content_left = document.getElementById('content_left');

while (!content_left.firstChild.className || !content_left.firstChild.className.match(/result/)) {
    content_left.removeChild(content_left.firstChild);
}
