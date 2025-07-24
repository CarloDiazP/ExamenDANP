# COVID-19 Contact Tracing App - UNSA

## Integrantes
- Carrillo Daza, Bárbara Rubí (100%)
- Diaz Portilla, Carlo Rodrigo (100%) 
- Mamani Cañari, Gabriel Antony (100%)
- Ticona Hareth, Anthony Joaquin (100%)

## Descripción del Proyecto

Aplicación Android de rastreo de contactos para COVID-19 desarrollada para el curso de Desarrollo Avanzado en Nuevas Plataformas. La aplicación detecta dispositivos cercanos usando Bluetooth Low Energy (BLE) sin comprometer la privacidad del usuario.

## Arquitectura

La aplicación utiliza arquitectura MVVM con los siguientes componentes:
- **Android App con BLE** para detección de proximidad
- **Firebase Firestore** para almacenamiento en la nube
- **Room Database** para almacenamiento local
- **WorkManager** para sincronización periódica
- **Firebase Cloud Messaging** para notificaciones

## Análisis de Tecnologías

### Bluetooth Low Energy (BLE) [SELECCIONADA]
- **Ventajas**: Excelente eficiencia energética, no revela ubicación GPS, alcance ideal de ~30m
- **Desventajas**: Señal puede variar por obstáculos

## Tecnologías y Temas del Curso Aplicados

1. **Almacenamiento Local (Tema 12)**: Room Database para contactos temporales
2. **Networking/API REST (Tema 10)**: Sincronización con Firebase
3. **Sincronización de Datos (Tema 12)**: WorkManager para sync periódico
4. **Notificaciones Push/FCM (Tema 13)**: Alertas de exposición
5. **Comunicación Bluetooth (Tema 14)**: Core de la solución con BLE

## Características principales

- IDs temporales rotativos cada 15 minutos
- Sin información personal identificable
- Sin rastreo de ubicación GPS
- Datos encriptados localmente
- Retención de datos limitada a 14 días

## Configuración del Proyecto

### Prerequisitos
- Android con Jetpack Compose
- Android SDK 29+ (target SDK 35)
- Dispositivo con Bluetooth LE

### Instalación

1. **Clonar el repositorio**
```bash
git clone https://github.com/CarloDiazP/ExamenDANP
cd ExamenDANP
```

2. **Configurar Firebase**
   - Crear proyecto en [Firebase Console](https://console.firebase.google.com)
   - Registrar app con package: `com.unsa.examendanp`
   - Descargar `google-services.json` y colocar en `app/`
   - Habilitar Firestore Database
   - Habilitar Cloud Messaging

3. **Configurar Firestore Rules**
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Permitir todo para desarrollo (CAMBIAR EN PRODUCCIÓN)
    match /{document=**} {
      allow read, write: if true;
    }
  }
}

```

4. **Ejecutar la aplicación**
   - Abrir proyecto en Android Studio
   - Sincronizar Gradle
   - Ejecutar en 2+ dispositivos para probar

## Uso de la Aplicación

1. **Primera ejecución**: La app solicitará permisos necesarios
2. **Activar rastreo**: Toggle switch para iniciar detección
3. **Visualización**: Ver contactos detectados en las últimas 24 horas
4. **Sincronización**: Botón para sincronizar manualmente

## Estructura del Proyecto

```
app/
├── data/               # Capa de datos
│   ├── local/         # Room, DataStore
│   ├── remote/        # Firebase
│   └── repository/    # Repositorios
├── domain/            # Lógica de negocio
│   ├── model/        # Modelos de dominio
│   └── usecase/      # Casos de uso
├── presentation/      # UI Layer
│   ├── ui/           # Screens y ViewModels
│   └── navigation/   # Navegación
├── services/         # Servicios en background
│   ├── bluetooth/    # BLE scanning
│   └── sync/         # WorkManager
└── utils/            # Utilidades
```

## Demostración

### Pantallas principales:
1. **Home Screen**: Estado del rastreo, contador de contactos, estado COVID
2. **Permisos**: Solicitud automática de permisos necesarios
3. **Notificaciones**: Alertas de exposición vía FCM

### Flujo de datos:
1. BLE detecta dispositivos cercanos → 
2. Almacena en Room Database → 
3. WorkManager sincroniza con Firestore → 
4. Autoridades analizan datos → 
5. FCM envía notificaciones de exposición (se requiere un cliente externo, no considerado en el aplicativo)

## Consideraciones de Batería

- Escaneo BLE en modo LOW_POWER
- Ciclos de escaneo/pausa de 10 segundos
- Sincronización cada 15 minutos
- Servicio en foreground con notificación permanente

## Testing

Para probar la detección de proximidad:
1. Instalar en 2+ dispositivos Android
2. Activar Bluetooth y permisos
3. Iniciar rastreo en ambos dispositivos
4. Mantener dispositivos a <30m por >1 minuto
5. Verificar detección en la UI

