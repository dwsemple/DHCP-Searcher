$dhcpServers = Get-DhcpServerInDC
$objectHeadings = "IP" + "`t" + "Name"
$serverObjects = @()
$serverObjects += $objectHeadings
foreach($dhcpServer in $dhcpServers) {
$newServerObject = New-Object System.Object
$newServerObject | Add-Member -type NoteProperty -name IP -value "$($dhcpServer.IPAddress)"
$newServerObject | Add-Member -type NoteProperty -name Name -value "$($dhcpServer.DnsName)"
$newServerObject.IP = $newServerObject.IP -replace "`n|`r|`t",""
$newServerObject.Name = $newServerObject.Name -replace "`n|`r|`t",""
$newServerObjectString = "$($newServerObject.IP)" + "`t" + "$($newServerObject.Name)"
$serverObjects += $newServerObjectString
}
$serverObjects | Out-File -filepath data\servers\dhcp_servers.txt -encoding "ASCII"