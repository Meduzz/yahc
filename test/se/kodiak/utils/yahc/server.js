var http = require('http');

var server = http.createServer();
var buffer = require("buffer");

server.on('request', function(req, res){
  console.log("Got a request to: "+req.url);
  res.statusCode = 200;

  if (req.url == '/get') {
    console.log("200");
    res.write("This was a get request.", "utf8");
    res.end();
  } else if (req.url == '/post' && req.method == 'POST') {
    console.log("200");
    res.write("This was a post request.", "utf8");
    res.end();
  } else if (req.url == '/put' && req.method == 'PUT') {
    console.log("200");
    res.write("This was a put request.", "utf8");
    res.end();
  } else if (req.url == '/delete' && req.method == 'DELETE') {
    console.log("200");
    res.write("This was a delete request.", "utf8");
    res.end();
  } else if (req.url == '/head' && req.method == 'HEAD') {
    console.log("200");
    res.end();
  } else if (req.url == '/chunked' && req.method == 'GET') {
    console.log("200");
    res.write("chunk1");
    res.write("chunk2");
    res.end();
  } else if (req.url == '/slow' && req.method == 'GET') {
    setTimeout(function(){
      console.log("200");
      res.write("Slow response, sorry about that!");
      res.end();
    }, 1000);
  } else {
    console.log("404")
    res.statusCode = 404;
    res.write("404 - Not found.")
    res.end();
  }
});

server.listen(3000, function(){
  console.log("Started on port 3000.");
})