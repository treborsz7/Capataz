# Capataz - Aplicación Android de Gestión

## Descripción
Capataz es una aplicación Android moderna diseñada para la gestión y control de tareas operativas. Integra funcionalidades de autenticación, escaneo de códigos QR/barras y gestión de ubicaciones, proporcionando una solución completa para la supervisión y control de operaciones.

## Características Principales
- **Autenticación Segura**
  - Sistema de login con usuario y contraseña
  - Soporte para múltiples empresas
  - Recordatorio de credenciales
  - Manejo de tokens JWT para autenticación
  - Login automático (si está activado)

- **Gestión de Sesión**
  - Interceptor global para manejo de token
  - Almacenamiento seguro de credenciales
  - Cierre de sesión controlado

- **Escaneo de Códigos**
  - Lectura de códigos QR y códigos de barras
  - Procesamiento en tiempo real
  - Interfaz de cámara integrada

- **Gestión de Ubicaciones**
  - Registro de ubicaciones
  - Asignación de partidas
  - Seguimiento de movimientos

## Tecnologías y Frameworks
- **Arquitectura**
  - MVVM (Model-View-ViewModel)
  - Clean Architecture
  - Jetpack Compose para UI

- **Bibliotecas Principales**
  - Retrofit2 para comunicación HTTP
  - OkHttp3 para interceptores y manejo de red
  - CameraX para el escaneo de códigos
  - MLKit para el procesamiento de códigos QR/barras
  - Jetpack Components
    - ViewModel
    - Compose
    - Navigation

- **Almacenamiento**
  - SharedPreferences para datos de sesión
  - TokenProvider para gestión global del token

## Requisitos del Sistema
- Android 6.0 (API 23) o superior
- Cámara con autofocus
- Conexión a Internet

## Configuración
1. Clone el repositorio
2. Abra el proyecto en Android Studio
3. Configure el archivo `gradle.properties` con las variables de entorno necesarias
4. Ejecute la aplicación

## Uso
1. Inicie la aplicación
2. Ingrese sus credenciales (o use el login automático si está configurado)
3. Seleccione la empresa
4. Acceda a las funcionalidades de escaneo y gestión

## Estructura del Proyecto
```
app/
├── src/
│   ├── main/
│   │   ├── java/com/codegalaxy/barcodescanner/
│   │   │   ├── model/          # Modelos de datos
│   │   │   ├── service/        # Servicios y API
│   │   │   ├── view/           # Activities y Screens
│   │   │   ├── viewmodel/      # ViewModels
│   │   │   └── ui/theme/       # Temas y estilos
│   │   └── res/               # Recursos
│   └── androidTest/           # Pruebas de instrumentación
└── build.gradle.kts          # Configuración del módulo
```

