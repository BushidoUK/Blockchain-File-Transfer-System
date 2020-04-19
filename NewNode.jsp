<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.UUID"%>
<%@page session="true" %>

<!DOCTYPE html>
<html>
<head>

<title>File Cube's BCFT System</title>

    <link rel="icon" type="image/png" href="images/icons/favicon.ico"/>
    <link rel="stylesheet" type="text/css" href="vendor/bootstrap/css/bootstrap.min.css">
    <link rel="stylesheet" type="text/css" href="fonts/font-awesome-4.7.0/css/font-awesome.min.css">
    <link rel="stylesheet" type="text/css" href="vendor/animate/animate.css">
    <link rel="stylesheet" type="text/css" href="vendor/css-hamburgers/hamburgers.min.css">
    <link rel="stylesheet" type="text/css" href="vendor/select2/select2.min.css">
    <link rel="stylesheet" type="text/css" href="css/util.css">
    <link rel="stylesheet" type="text/css" href="css/main.css">

    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
</head>
    <body>
    <div class="limiter">
    <div class="container-login100">
    <div class="wrap-login100">
    <div class="login100-pic js-tilt" data-tilt><img src="images/filecubelogo.png" alt="IMG">
    </div>

    <h1>New Node Creation:</h1>
    
    <% 
//check the session to see if user is logged in and if the "uname" is in the database
String uname=(String)session.getAttribute("uname"); 
//redirect user to login page if not logged in
if(uname==null)
{
    //when there is no user name then redirect to index
    response.sendRedirect("index.jsp");
} 
%>  

<%
    out.println(String.format("Welcome, %s" ,(String)session.getAttribute("uname")));
%>

    <div>
    <a class="login100-form-btn"  href="http://localhost:8080/BCFT_v3/WelcomeUser.jsp"> Main Menu</a>
    </div>
    
<form method="post" action="AddNode">
<centre>
    <table>
        <tbody>
<tr> 
<td> Hostname: <input class="input100" type="text" name="user" value="" /></td>
</tr>

<tr>
<td> Password: <input class="input100" type="password" name="pass" value="" /></td>
</tr>

<tr>
<td>IP Address: <input class="input100" type="text" name="address" value="" /></td>
</tr>

<tr>
<td><input class="login100-form-btn" type="submit" value="Submit" /></td>
<td><input class="login100-form-btn" type="reset" value="Reset" /></td>
</tr>

        </tbody>
    </table>
</centre>
    
</form>
    
</body>

</html>

