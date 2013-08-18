var http = require('http');

var server = http.createServer();
var buffer = require("buffer");

server.on('request', function(req, res){
  console.log("Got a request to: "+req.url);
  res.statusCode = 200;

  if (req.url == '/get') {
    console.log("200");
    res.write("This was a get request.", "utf8");
  } else if (req.url == '/post' && req.method == 'POST') {
    console.log("200");
    res.write("This was a post request.", "utf8");
  } else {
    console.log("404")
    res.statusCode = 404;
    res.write("404 - Not found.")
  }

  res.end();
});

server.listen(3000, function(){
  console.log("Started on port 3000.");
})