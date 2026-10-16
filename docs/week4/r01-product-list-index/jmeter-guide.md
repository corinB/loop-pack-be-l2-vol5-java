# JMeter 변경 전 측정: 실행하고 보고서 읽기

[측정 설정](trade_off/04-jmeter-run-settings.md) · [JMX](jmeter/product-list.jmx) · [요구사항](requirement.md)

**API와 JMeter 준비는 끝났다. 본 측정은 아래 명령으로 직접 실행하면 된다.**
세 시나리오의 1명·1회 full smoke는 이미 통과했다. 추가 smoke는 실행하지 않으며 본 측정 결과는 아직 없다.

## 1. 준비된 API로 측정 시작

PowerShell에 아래 명령을 붙여 넣는다. 실행하면 워밍업과 모든 본 측정이 자동으로 이어진다.
Windows PowerShell 5.1에서도 한글 경로를 읽을 수 있도록 `-Encoding UTF8`을 포함한다.

```powershell
Set-Location -LiteralPath 'C:\Users\qorwh\OneDrive\바탕 화면\Loop\loop-pack-be-l2-vol5-java'
$bench = '.\docs\week4\r01-product-list-index\jmeter'
$meta = (Get-Content '.\build\benchmark\current-measure-api-path.txt' -Raw -Encoding UTF8).Trim()
$result = & "$bench\run-jmeter.ps1" -Mode Measure -Phase before -ApiMetadataPath $meta
$result.OutputDirectory
$result.ManifestPath
```

Docker와 준비된 API는 켜 둔다. JMeter GUI와 다른 부하 테스트·빌드는 닫거나 중단한 상태에서 실행한다.
스크립트가 실제 API 프로세스, 전용 DB, 기존 인덱스 4개, MySQL 버퍼 풀 128MiB를 확인한다.

| 기본 설정 | 값 |
|---|---|
| 시나리오 | 전체 최신순 / 전체 가격순 / 브랜드 7 가격순 |
| 요청 | 모두 `page=0&size=20` |
| 동시 사용자 | 1명, 10명을 각각 실행 |
| 각 run | 워밍업 20초 + 본 측정 60초 |
| 반복 | 각 조합 2회, 본 측정 총 12개 |
| 전체 예상 시간 | 설정 시간 16분 + 실행·보고서 생성, 약 17~20분 |

ramp-up 5초는 각 실행 시간 안에 포함된다. 목표 동시 수가 유지되는 구간을 60초 확보하는 설정은 아니다.
실제 소요 시간은 응답 종료·프로세스 시작·보고서 생성 등에 따라 더 길어질 수 있다.

## 2. 완료 후 HTML 열기

같은 PowerShell 창에서 첫 번째 본 측정 보고서를 연다.

```powershell
$report = Join-Path $result.OutputDirectory 'r01-latest-u1\measurement\html\index.html'
Invoke-Item -LiteralPath $report
```

출력 폴더는 `build/benchmark/jmeter/before-measure-실행시각-…/`다.
`r01-latest-u1`은 **1회차·최신순·동시 1명**이고, `price`, `brand7`, `u10`, `r02`도 같은 방식으로 읽는다.

```text
manifest.json
r01-latest-u1/
  warmup/       samples.jtl, jmeter.log, console.log, arguments.json
  measurement/  samples.jtl, jmeter.log, console.log, arguments.json, html/index.html
```

HTML은 `measurement` 폴더에만 있다. 워밍업 JTL은 본 측정에 합치지 않는다.

## 3. 보고서에서 읽을 값

| 항목 | 의미 |
|---|---|
| Samples | 실제 HTTP 요청 수. 상품 개수와 다름 |
| Error % | 실패한 요청 비율. 먼저 0인지 확인 |
| Average | 평균 응답 시간(ms) |
| Median | p50, 요청 절반이 이 시간 안에 끝남 |
| 95th pct / 99th pct | p95 / p99 응답 시간(ms) |
| Throughput | 단위 시간당 처리량. 보고서의 시간 단위 확인 |

기본 HTML의 `90th pct`는 p90이며 p50은 `Median`이다.
같은 시나리오·동시 수의 두 반복을 나란히 보고 편차를 확인한다. 다른 조합을 하나의 평균으로 합치지 않는다.
표본이 적은 p99나 실패가 있는 실행은 그대로 한계를 기록한다.

이 부하는 응답이 끝난 뒤 다음 요청을 보내며 think time·TPS 제한은 없다.
동시 1명·10명에서 관찰한 지연·처리량으로 해석한다. 자세한 설계와 한계는 [측정 설정](trade_off/04-jmeter-run-settings.md)을 따른다.

## 결과 보관과 오류 확인

`$result.OutputDirectory`의 **폴더 전체**를 보관한다. JTL은 요청별 원본, manifest는 전체 설정·결과, HTML은 보고서다.
결과는 Git에서 제외된 `build/` 아래에 있으므로 `clean` 전에 별도 보관한다.

- 실행 실패: 콘솔에 나온 결과 폴더의 `manifest.json`, 해당 단계의 `console.log`, `jmeter.log`, `samples.jtl`, `arguments.json`을 확인한다.
- API 오류: metadata와 같은 폴더의 `stdout.log`, `stderr.log`를 확인한다.
- 기존 결과를 지우거나 seed·DDL·DB 설정을 바꾸지 말고 오류 원본을 남긴다.

## API가 꺼졌거나 새 창을 열었을 때

API가 꺼졌거나 현재 실행 정보 파일이 없을 때만 새로 시작한다. 정상 상태는 `TcpListening=True`다.

```powershell
$api = & "$bench\start-benchmark-api.ps1"
$meta = $api.MetadataPath
$api | Select-Object ProcessId, Status, TcpListening
```

새 PowerShell 창에서는 1단계의 저장소 이동과 `$bench` 선언 뒤 현재 실행 정보를 다시 읽는다.

```powershell
$meta = (Get-Content '.\build\benchmark\current-measure-api-path.txt' -Raw -Encoding UTF8).Trim()
Get-Content -LiteralPath $meta -Raw -Encoding UTF8 | ConvertFrom-Json | Select-Object ProcessId, Status
```

이미 켜진 API는 다시 시작하지 않는다. 스크립트가 실제 프로세스를 확인하며, 18080 포트가 사용 중이면 임의 종료하지 않는다.
앱은 전용 DB·`ddl-auto=none`·집계 OFF·COUNT TTL `0s`·SQL 로그 OFF로 실행된다. seed·스키마 생성은 다시 하지 않는다.

측정용 API를 종료할 때는 다음 명령을 사용한다. metadata의 프로세스가 맞는지 확인한 뒤 종료한다.

```powershell
& "$bench\stop-benchmark-api.ps1" -MetadataPath $meta
```

결과 변수가 사라졌다면 파일 탐색기에서 해당 실행의 `measurement/html/index.html`을 연다.

## GUI는 선택 사항

공식 ZIP의 SHA-512를 확인한 JMeter 5.6.3은 저장소의 `apache-jmeter-5.6.3/`에 있고 실행 스크립트는 기존 JDK 21을 사용한다.
GUI는 JMX 구성 확인용으로만 열어도 충분하다. 본 측정은 위 CLI 명령으로 진행하고 실행 전 GUI를 닫는다.
[Apache JMeter 실행 안내](https://jmeter.apache.org/usermanual/get-started.html) · [공식 권장 사항](https://jmeter.apache.org/usermanual/best-practices.html#lean_mean)
