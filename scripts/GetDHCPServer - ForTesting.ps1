$dhcpServers = Get-DhcpServerInDC
$objectHeadings = "IP" + "`t" + "Name"
$noServer1 = "ahdc01.baysidehealth.intra"
$noServer2 = "ahdc02.baysidehealth.intra"
$serverObjects = @()
$serverObjects += $objectHeadings
foreach($dhcpServer in $dhcpServers) {
if(("$($dhcpServer.DnsName)" -notmatch $noServer1) -and ("$($dhcpServer.DnsName)" -notmatch $noServer2)) {
$newServerObject = New-Object System.Object
$newServerObject | Add-Member -type NoteProperty -name IP -value "$($dhcpServer.IPAddress)"
$newServerObject | Add-Member -type NoteProperty -name Name -value "$($dhcpServer.DnsName)"
$newServerObject.IP = $newServerObject.IP -replace "`n|`r|`t",""
$newServerObject.Name = $newServerObject.Name -replace "`n|`r|`t",""
$newServerObjectString = "$($newServerObject.IP)" + "`t" + "$($newServerObject.Name)"
$serverObjects += $newServerObjectString
}
}
$serverObjects | Out-File -filepath data\servers\dhcp_servers.txt -encoding "ASCII"