param([string]$scope,[string]$server)
$leasedIPs = Get-DHCPServerv4Lease -ScopeId $scope -computername $server | Where {$_.AddressState -eq "Active"}
$objectHeadings = "IP" + "`t" + "Hostname" + "`t" + "MAC" + "`t" + "Expiry"
$leaseObjects = @()
$leaseObjects += $objectHeadings
foreach($lease in $leasedIPs) {
$newLeaseObject = New-Object System.Object
$newLeaseObject | Add-Member -type NoteProperty -name IP -value "$($lease.IPAddress)"
$newLeaseObject | Add-Member -type NoteProperty -name Hostname -value "$($lease.HostName)"
$newLeaseObject | Add-Member -type NoteProperty -name MAC -value "$($lease.ClientId)"
$newLeaseObject | Add-Member -type NoteProperty -name Expiry -value "$($lease.LeaseExpiryTime)"
$newLeaseObject.IP = $newLeaseObject.IP -replace "`n|`r|`t",""
$newLeaseObject.Hostname = $newLeaseObject.Hostname -replace "`n|`r|`t",""
$newLeaseObject.MAC = $newLeaseObject.MAC -replace "`n|`r|`t|-",""
$newLeaseObject.Expiry = $newLeaseObject.Expiry -replace "`n|`r|`t|-",""
$newLeaseObjectString = "$($newLeaseObject.IP)" + "`t" + "$($newLeaseObject.Hostname)" + "`t" + "$($newLeaseObject.MAC)" + "`t" + "$($newLeaseObject.Expiry)"
$leaseObjects += $newLeaseObjectString
}
$leaseObjects