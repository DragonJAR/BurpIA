$inputStream = [Console]::OpenStandardInput()
Write-Output "ENTER_PROBE_READY os=windows pid=$PID"

$buffer = New-Object System.Collections.Generic.List[byte]
while (($byte = $inputStream.ReadByte()) -ne -1) {
    $hex = '{0:X2}' -f $byte
    if ($byte -eq 13 -or $byte -eq 10) {
        $text = [System.Text.Encoding]::UTF8.GetString($buffer.ToArray())
        Write-Output ("ENTER_PROBE_SIGNAL byte={0} len={1}" -f $hex, $text.Length)
        Write-Output ("ENTER_PROBE_BUFFER text={0}" -f $text)
        $buffer.Clear()
    } else {
        $buffer.Add([byte]$byte)
    }
}

Write-Output "ENTER_PROBE_EOF"
