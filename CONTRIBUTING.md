# Guía para Contribuir a BelZSpeedScan

¡Gracias por tu interés en contribuir a BelZSpeedScan! Este documento proporciona las directrices y el proceso para contribuir al proyecto.

## 🎯 ¿Por qué contribuir?

BelZSpeedScan es una librería de escaneo de códigos de barras y QR que busca hacer el proceso de escaneo más rápido y eficiente. Tu contribución puede ayudar a:

- Mejorar la experiencia de los usuarios
- Añadir nuevas funcionalidades
- Corregir bugs
- Mejorar la documentación
- Optimizar el rendimiento

## 🚀 ¿Cómo empezar?

1. **Fork del repositorio**
   - Haz click en el botón "Fork" en la parte superior derecha de la página del repositorio

2. **Clona tu fork**
   ```bash
   git clone https://github.com/TU-USUARIO/BelZSpeedScan.git
   cd BelZSpeedScan
   ```

3. **Configura el entorno de desarrollo**
   - Asegúrate de tener instalado:
     - Android Studio (última versión estable)
     - Xcode (para desarrollo iOS)
     - Kotlin 1.8.0 o superior
     - Gradle 7.0 o superior

4. **Crea una rama para tu feature**
   ```bash
   git checkout -b feature/nombre-de-tu-feature
   ```

## 📝 Proceso de Contribución

1. **Desarrollo**
   - Sigue las convenciones de código existentes
   - Añade tests para nuevas funcionalidades
   - Actualiza la documentación según sea necesario

2. **Commits**
   - Usa mensajes de commit descriptivos
   - Sigue el formato: `tipo(alcance): descripción`
   - Ejemplo: `feat(android): añade soporte para escaneo en segundo plano`

3. **Pull Request**
   - Asegúrate de que tu código compila y pasa todos los tests
   - Actualiza la documentación si es necesario
   - Describe claramente los cambios en el PR
   - Referencia cualquier issue relacionado

## 🎨 Guías de Estilo

### Kotlin
- Sigue las [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Usa `camelCase` para variables y funciones
- Usa `PascalCase` para clases y objetos
- Mantén las funciones cortas y enfocadas

### Documentación
- Documenta todas las APIs públicas
- Usa KDoc para documentación de código
- Mantén el README actualizado

## 🧪 Testing

- Escribe tests unitarios para nueva funcionalidad
- Asegúrate de que todos los tests pasan antes de enviar un PR
- Mantén la cobertura de tests por encima del 80%

## 📋 Checklist para Pull Requests

- [ ] El código sigue las guías de estilo
- [ ] Se han añadido tests para nueva funcionalidad
- [ ] La documentación ha sido actualizada
- [ ] Los tests pasan localmente
- [ ] El código compila sin warnings
- [ ] Se han resuelto conflictos con la rama principal

## 🤝 Código de Conducta

Por favor, lee nuestro [Código de Conducta](CODE_OF_CONDUCT.md) antes de contribuir.

## ❓ ¿Necesitas ayuda?

- Abre un issue para preguntas o problemas
- Únete a nuestras discusiones en GitHub
- Contacta a los mantenedores del proyecto

## 📜 Licencia

Al contribuir, aceptas que tu código será licenciado bajo la misma licencia que el proyecto (MIT).

---

¡Gracias por contribuir a BelZSpeedScan! 🎉 