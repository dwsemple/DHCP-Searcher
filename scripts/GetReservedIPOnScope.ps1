param([string]$scope,[string]$server)
$reservedIPs = Get-DHCPServerv4Lease -ScopeId $scope -computername $server | Where {$_.AddressState -eq "ActiveReservation"}
$objectHeadings = "IP" + "`t" + "Hostname" + "`t" + "MAC"
$reservationObjects = @()
$reservationObjects += $objectHeadings
foreach($reservation in $reservedIPs) {
$newReservationObject = New-Object System.Object
$newReservationObject | Add-Member -type NoteProperty -name IP -value "$($reservation.IPAddress)"
$newReservationObject | Add-Member -type NoteProperty -name Hostname -value "$($reservation.HostName)"
$newReservationObject | Add-Member -type NoteProperty -name MAC -value "$($reservation.ClientId)"
$newReservationObject.IP = $newReservationObject.IP -replace "`n|`r|`t",""
$newReservationObject.Hostname = $newReservationObject.Hostname -replace "`n|`r|`t",""
$newReservationObject.MAC = $newReservationObject.MAC -replace "`n|`r|`t|-",""
$newReservationObjectString = "$($newReservationObject.IP)" + "`t" + "$($newReservationObject.Hostname)" + "`t" + "$($newReservationObject.MAC)"
$reservationObjects += $newReservationObjectString
}
$scopeFilenameFriendly = $scope.Replace(".", "_")
$serverFilenameFriendly = $server.Replace(".", "_")
$filename = "data\\reservations\\$($serverFilenameFriendly)_scope_$($scopeFilenameFriendly).txt"
$reservationObjects | Out-File -filepath $filename -encoding "ASCII"