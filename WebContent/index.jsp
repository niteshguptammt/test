<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
    "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Hello World Java EE</title>
</head>
<body>
    <h1>Hello JSP and Servlet!</h1>

<form action="webSocketServlet" method="post">
    Enter your name: <input type="text" name="yourName" size="20">
    <input type="submit" value="Call Servlet" />
    <!-- <input type="submit" value="Call Servlet" OnClick="Hello()" /> -->
</form>

<form action="" >
    Enter your id: <input type="text" name="yourId" size="20">
    <!-- <input type="submit" value="Call Servlet" /> -->
    <input type="submit" value="Call javascript" OnClick="Hello()" />
    <table>
<tr>
<td> <label id="rateLbl">Current Rate:</label></td>
<td> <label id="rate">0</label></td>
</tr>
</table>
</form>

<SCRIPT LANGUAGE="JavaScript">


    function Hello ()
    {
        alert("Hello World!")
    }
    var wsocket;      
    function connect() {         
		wsocket = new WebSocket("ws://localhost:8080/WebSocketTest/ratesrv");       
        wsocket.onmessage = onMessage;          
    }
    function onMessage(evt) {             
       document.getElementById("rate").append(evt.data);          
    }
window.addEventListener("load", connect, false);

</SCRIPT>
</body>
</html>