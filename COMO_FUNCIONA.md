# 09 — App Piloto Uber / 99 Motorista (v1.2 — Android Kotlin)
Aplicativo Android nativo para motoristas da Uber e 99, com build automático de APK via GitHub Actions (.github/workflows/build-apk.yml).
- **PilotAccessibilityService.kt:** captura na tela ofertas de corrida da Uber e 99.
- **RideOfferParser.kt:** extrai valor, distância, tempo e destino.
- **RideRuleEvaluator.kt (+ testes):** calcula R$/km e R$/hora conforme metas do motorista.
- **CityFilter.kt:** filtra cidades/regiões desejadas ou bloqueadas.
- **OverlayService.kt:** mostra selo flutuante colorido sobre o app com o veredito da corrida.
- **VoiceAnnouncer.kt:** anuncia por voz a análise da corrida.
- **AppSwitcher.kt:** alternância rápida entre Uber e 99.
