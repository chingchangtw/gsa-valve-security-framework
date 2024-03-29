<html>
	<head>
		<title>Login - Double authentication login form</title>
	</head>
	
	<!-- DESCRIPTION
	     loginkrb.jsp is the default login page when using Form Based AuthN (SSO) approach
	     with Kerberos and you choose to have double authentication (username/password thru a login form)
	     It's similar to loginkrbSAML.jsp but for Forms-based scenarios
	     You can customize it and change its name as long as you update it in config files
	-->
		
	<body>
	
	<!-- Customize Look and Feel -->
	
		<center>
		<form method="post" action="/valve/kerberos" name="login_form">
			<table width="300" border="0" align="center" cellpadding="2" cellspacing="1">
				
				<tr>
					<td>
						<table width="100%" border="0" cellspacing="1" cellpadding="1">
							<tr>
								<td><div align="right"><strong><font color="#333333" size="-1" face="Arial, Helvetica, sans-serif">Username</font></strong></div></td>
								<td><input type="text" name="UserIDKrb" size="30" maxlength="30"></td>
							</tr>
							<tr>
								<td><div align="right"><strong><font color="#333333" size="-1" face="Arial, Helvetica, sans-serif">Password</font></strong></div></td>
								<td><input type="password" name="PasswordKrb" size="30" maxlength="30"></td>
							</tr>
							<tr>
								<td>&nbsp;</td>
								<td><input type="submit" name="Submit" style="background-color: transparent;" value="Enter"></td>
							</tr>
						</table>
					</td>
				</tr>
				
			</table>
		</form>
		</center>
	</body>
</html>