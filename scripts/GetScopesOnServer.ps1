param([string]$server)
$scopes = Get-DHCPServerv4scope -computername $server | Where {$_.State -eq "Active"} | Select-Object -Property ScopeId, Name
$objectHeadings = "Scope" + "`t" + "Name"
$scopeObjects = @()
$scopeObjects += $objectHeadings
foreach($scope in $scopes) {
$newScopeObject = New-Object System.Object
$newScopeObject | Add-Member -type NoteProperty -name Scope -value "$($scope.ScopeId)"
$newScopeObject | Add-Member -type NoteProperty -name Name -value "$($scope.Name)"
$newScopeObject.Scope = $newScopeObject.Scope -replace "`n|`r|`t",""
$newScopeObject.Name = $newScopeObject.Name -replace "`n|`r|`t",""
$newScopeObjectString = "$($newScopeObject.Scope)" + "`t" + "$($newScopeObject.Name)"
$scopeObjects += $newScopeObjectString
}
$serverFilenameFriendly = $server.Replace(".", "_")
$filename = "data\\scopes\\$($serverFilenameFriendly).txt"
$scopeObjects | Out-File -filepath $filename -encoding "ASCII"