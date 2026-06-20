# 🐄 Vaka

> Una app de ahorro colaborativo para hacer la vaca con tus amigos, sin enredos.

Vaka es una app Android para ahorrar de forma compartida o individual. Crea metas, registra aportes y haz vakas con tus amigos por código o QR. Funciona en multimoneda con conversión automática.

---

## ✨ Funcionalidades principales

### 💰 Ahorro y metas
- **Vakas privadas** ilimitadas con meta opcional y fecha límite
- **Vakas compartidas** con amigos por código de 6 caracteres o QR
- **Multi-moneda:** 10+ monedas con conversión automática (tasas diarias)
- **Plan automático de meta:** te dice cuánto aportar por día/semana/mes
- **Aportes recurrentes:** programa aportes automáticos diarios, semanales o mensuales
- **Mover dinero entre Vakas** sin perder el historial

### 🤝 Social
- Sistema de amigos con código único `VK-XXXXXX`
- Vakas compartidas con división equitativa automática
- Ranking de aportes y movimientos por miembro
- Notificaciones en tiempo real cuando un amigo aporta

### 🎯 Motivación
- 9 logros desbloqueables (primer depósito, constancia, meta cumplida, etc.)
- 4 tipos de retos semanales rotativos
- Sistema de racha (semanas consecutivas ahorrando)
- Notificaciones motivacionales diarias

### 🎨 Personalización
- 8 colores de avatar personalizables (afecta toda la app)
- Tema claro, oscuro o según el sistema
- Foto de perfil personalizada
- Moneda principal a tu elección
- Vakas favoritas que aparecen primero

### 🔔 Inteligente
- Notificaciones limitadas a 1/día para no abrumar
- Modo "no me molestes" con pausa temporal
- Sistema OTA para actualizaciones automáticas
- Detección automática de logros y celebración con confeti
- Onboarding suave para usuarios nuevos

---

## 🛠️ Tecnologías

- **Lenguaje:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Backend:** Firebase (Auth + Firestore)
- **Auth:** Email/contraseña + Google Sign-In (Credential Manager)
- **Almacenamiento local:** SharedPreferences
- **Conversión de monedas:** ExchangeRate-API (gratis, sin clave)
- **QR:** ZXing
- **Notificaciones:** WorkManager

---

## 📋 Requisitos

- Android 8.0 (API 26) o superior
- Conexión a internet (para Vakas compartidas y conversión de monedas)
- Cuenta de Google o email (opcional, también funciona como invitado)

---

## 🚀 Instalación

### Para usuarios
Descarga el APK desde la sección [Releases](https://github.com/Matallana14/vaka/releases) de este repositorio. La app tiene actualizaciones automáticas (OTA) integradas.

### Para desarrolladores

1. Clona el repositorio:
   ```bash
   git clone https://github.com/Matallana14/vaka.git
   ```

2. Abre el proyecto en Android Studio (versión 2023.1+ recomendada)

3. **Configura Firebase:**
   - Crea un proyecto en [Firebase Console](https://console.firebase.google.com)
   - Activa Authentication (Email + Google)
   - Activa Firestore Database
   - Descarga `google-services.json` y colócalo en `app/`

4. **Configura el keystore para firmar APKs:**
   - Copia `keystore.properties.example` a `keystore.properties`
   - Llena con los datos de tu keystore propio

5. **(Opcional) Si quieres habilitar el sistema OTA:**
   - Edita `Actualizador.kt` con tu repo de releases y un token de GitHub

6. Sincroniza Gradle y compila.

---

## 💱 Conversión de monedas

Vaka usa tasas de referencia diarias de [ExchangeRate-API](https://www.exchangerate-api.com) para mostrar tu patrimonio en una sola moneda cuando tienes Vakas en varias.

Son tasas de mercado (no oficiales de banco) y pueden diferir ligeramente del valor que te dé tu casa de cambio. Por eso los montos convertidos se muestran con el símbolo `≈` para indicar que son aproximaciones.

Las tasas se cachean localmente; si no hay internet, solo se suman las Vakas en tu moneda principal con un aviso claro.

---

## 🔐 Privacidad

- **Tus datos viven en tu teléfono.** Las Vakas privadas nunca salen de él.
- **Las Vakas compartidas** se sincronizan en Firebase con reglas de seguridad estrictas: solo los miembros del grupo pueden leerlas o editarlas.
- **No recolectamos analytics.** No hay tracking de comportamiento.
- **No hay anuncios.**
- **Sin terceros.** No vendemos ni compartimos información con nadie.

---

## 🗺️ Roadmap

- [ ] Notas/comentarios en Vakas compartidas
- [ ] Color personalizado por cada Vaka
- [ ] Versión iOS (a futuro)
- [ ] Soporte multi-idioma (español/inglés)

---

## 🤝 Contribuir

¿Encontraste un bug o tienes una idea? Abre un [issue](https://github.com/Matallana14/vaka/issues) o un pull request.

---

## 📄 Licencia

Este proyecto se distribuye bajo licencia MIT. Eres libre de usarlo, modificarlo y distribuirlo, siempre que mantengas el aviso de copyright original.

---

## 👤 Autor

Hecho con 🐄 y mucho café por [Mario Matallana](https://github.com/Matallana14) desde Bogotá, Colombia.

Si te gusta el proyecto, dale una ⭐ al repo para apoyar.
