# Juego de Cartas PUT0 🎴

[![Android](https://img.shields.io/badge/Plataforma-Android%206.0+-green.svg)](https://www.android.com)
[![Java](https://img.shields.io/badge/Lenguaje-Java%208-orange.svg)](https://www.java.com)
[![Estado](https://img.shields.io/badge/Estado-Alpha%201.0.0-blue.svg)](https://github.com/yourusername/puto-game)
![Release](https://img.shields.io/badge/Release-Q4_2025-important)

## 📱 Sobre el Proyecto

PUT0 es un emocionante juego de cartas multijugador desarrollado para Android, que combina estrategia, suerte y mecánicas de juego dinámicas. El juego utiliza dos mazos de póker estándar y presenta múltiples fases de juego, haciendo que cada partida sea única y entretenida.

**Cronograma de Lanzamiento:**
- Alpha 1.0: Disponible Actualmente
- Lanzamiento Completo: Previsto para finales de 2025

## 🎮 Descripción General

PUT0 se juega con:
- 2 mazos de póker estándar (104 cartas en total)
- Un mazo con reverso rojo y otro negro
- Palos tradicionales: ♠ ♥ ♣ ♦
- 2-6 jugadores

## 📋 Preparación del Juego

### Reparto Inicial
1. Cada jugador recibe 3 cartas boca abajo (ocultas para todos los jugadores)
2. Segunda ronda: 3 cartas boca arriba (visibles para todos)
3. Tercera ronda: 3 cartas boca abajo con opciones especiales:
   - Los jugadores pueden ver estas cartas
   - Opción de reemplazar las cartas de la segunda ronda
   - Si se reemplazan, las cartas de la tercera ronda se vuelven visibles
   - Si se mantienen, las cartas de la tercera ronda permanecen ocultas

> *Máximo de cartas en mano: 9 cartas por jugador*

## 🃏 Mecánica del Juego

### Orden de las Cartas
 
> ```2 < 3 < 4 < 5 < 6 < 7 < 8 < 9 < 10 < J < Q < K < A```

> *Nota: A, 2 y 10 tienen propiedades especiales y pueden usarse en cualquier situación*

### Flujo del Juego
1. Se selecciona una carta aleatoria para comenzar
2. El juego continúa en sentido horario
3. Los jugadores deben jugar cartas de igual o mayor valor
4. Se roba una carta después de jugar (mientras haya disponibles en el mazo)

## 🔄 Fases del Juego

### Fase 1: Mazo Inicial
- Jugar y robar del mazo inicial
- Continuar hasta que el mazo se agote
- Usar las cartas restantes en mano una vez que el mazo esté vacío

### Fase 2: Segundo Mazo
- Se activa cuando un jugador se queda sin cartas
- El jugador puede elegir el segundo mazo visible u oculto
- Debe revelar al menos una carta
- Las otras cartas permanecen ocultas para los oponentes

### Fase 3: Cartas Ocultas
- Disponible después de completar la Fase 2
- Robar carta oculta al azar
- Si la carta robada es menor que la carta en la mesa:
   - El jugador debe recoger todas las cartas visibles
   - El turno pasa al siguiente jugador


#### Fase Final Adicional

- Si en la última jugada un jugador lanza su última carta pero, debido a la jugada, debe recoger las cartas visibles de la mesa, entra en una **fase extra**.
- En esta **fase extra**, el jugador debe intentar **deshacerse de las cartas acumuladas**:
  - Debe jugar todas las cartas de su mazo.
  - Si logra lanzar la última carta y, además, revela y juega las cartas ocultas, gana **automáticamente** el juego.


## 🎯 Reglas Especiales

### Poder del Diez
Al jugar un 10:
1. Se descartan todas las cartas visibles en la mesa
2. Se roba una carta extra del mazo inicial
3. Se juega una nueva carta para reiniciar el montón visible
4. Se roba una carta adicional si tienes 2 cartas en mano (durante la fase del mazo inicial)

### Descartes Automáticos
- Cuatro cartas iguales en la mesa = descarte automático
- Ejemplo: Cuatro 9 activan la eliminación automática

## 🏆 Cómo Ganar

La victoria se logra:
1. Siendo el primero en jugar todas las cartas
2. Completando con éxito cualquier fase final si se activa
3. Gestionando efectivamente tanto las cartas visibles como las ocultas

## 🛠️ Detalles Técnicos

- **Plataforma**: Android
- **Lenguaje de Programación**: Java
- **Versión Mínima de Android**: 6.0+
- **Versión Actual**: Alpha 1.0.0

## 📲 Instalación

> *Las instrucciones de instalación se proporcionarán en el lanzamiento completo*


## 🤝 ¡Únete a la Comunidad!

¡Estamos construyendo algo emocionante y queremos que seas parte de ello! 

### 🌟 ¿Por qué Contribuir?
- Ayuda a dar forma a nuevas características y mecánicas
- Conecta con otros apasionados del desarrollo de juegos
- ¡Deja tu huella en un proyecto que llegará a jugadores de todo el mundo!

### 🎮 Próximamente
- Sistema de logros y rankings
- Modos de juego especiales
- Torneos en línea
- ¡Y mucho más por venir!

### 📢 Mantente Informado
- Síguenos en nuestras redes sociales [próximamente]
- Únete a nuestro servidor de Discord [próximamente]
- Regístrate para la beta cerrada: [próximamente]

## 📝 Información Legal

### Licencia
Este proyecto estará disponible bajo una licencia que permitirá:
- Jugar y compartir libremente
- Contribuir al desarrollo
- Mantener la integridad del juego
> *Detalles completos de la licencia disponibles en el lanzamiento*

---

### 🚀 Roadmap 2025
- Q1 2025: Beta cerrada
- Q2 2025: Beta abierta
- Q3 2025: Release Candidate
- Q4 2025: ¡Lanzamiento oficial! 🎉

> *PUT0 está actualmente en fase Alpha 1.0.0. Estamos trabajando arduamente para pulir cada aspecto del juego. ¡Prepárate para una experiencia única que revolucionará tus partidas con amigos!*

---

#### 💫 "La próxima evolución de los juegos de cartas está llegando a tu dispositivo Android"