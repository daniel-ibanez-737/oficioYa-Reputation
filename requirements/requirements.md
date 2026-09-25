# 📄 Requerimientos del Sistema

## 1. Lista general de requerimientos

El sistema de Reputation (OficioYa) debe tener los siguientes requerimientos:

### 1.1 Requerimientos funcionales

1. Permitir registrar una calificación de 1 a 5 estrellas de cada una de las partes al finalizar un servicio.
2. Calcular y actualizar el promedio de calificación del trabajador cada vez que se registra una nueva reseña.
3. Mostrar las reseñas en el perfil del trabajador, sin permitir su edición o eliminación.
4. Permitir a un usuario reportar una reseña que considere falsa o inapropiada.
5. Pausar automáticamente a un trabajador cuyo promedio sea inferior a 3 estrellas en sus últimas 5 reseñas.
6. Aplicar una penalización de 0,5 puntos a la reputación del trabajador cuando cancele una solicitud después del límite de 24 horas.
7. Asociar cada calificación y reseña al servicio que la originó.

### 1.2 Requerimientos no funcionales

2. Cobertura de pruebas unitarias mínima del 80%.
3. La interfaz de calificación debe ser responsive.
4. El sistema debe registrar logs de cada calificación y cada pausado automático.
5. El identificador de cada reseña debe ser único y trazable al servicio que la originó.

## 2. Diagramas de caso de uso

### 2.1 Requerimiento Funcional 1

| Campo | Descripción |
|------|-------------|
| **ID** | RF-01 |
| **Nombre del requerimiento** | Registro de calificación al finalizar un servicio |
| **Descripción** | *El sistema debe permitir que trabajador y contratante se califiquen mutuamente de 1 a 5 estrellas cuando el servicio esté "Cumplida"* |
| **Precondiciones** | *La solicitud debe estar en estado Cumplida* |
| **Actor** | *Trabajador y Contratante* |
| **Flujo principal** | 1. El sistema habilita la opción de calificar.<br>2. Cada parte asigna de 1 a 5 estrellas.<br>3. El sistema guarda la reseña. |
| **Poscondiciones** | *La reseña queda registrada y lista para recalcular el promedio.* |

### 2.2 Requerimiento Funcional 2

| Campo | Descripción |
|------|-------------|
| **ID** | RF-02 |
| **Nombre del requerimiento** | Cálculo del promedio de reputación |
| **Descripción** | *El sistema debe recalcular el promedio del trabajador cada vez que se registra una nueva reseña* |
| **Precondiciones** | *Debe existir al menos una reseña registrada* |
| **Actor** | *Sistema (proceso automático)* |
| **Flujo principal** | 1. El sistema detecta una nueva reseña.<br>2. Recalcula el promedio general.<br>3. Actualiza el valor en el perfil. |
| **Poscondiciones** | *El promedio de reputación queda actualizado.* |

### 2.3 Requerimiento Funcional 3

| Campo | Descripción |
|------|-------------|
| **ID** | RF-03 |
| **Nombre del requerimiento** | Consulta de reseñas del perfil |
| **Descripción** | *El sistema debe mostrar las reseñas en el perfil del trabajador, sin permitir edición o eliminación* |
| **Precondiciones** | *El trabajador debe tener al menos una reseña* |
| **Actor** | *Contratante* |
| **Flujo principal** | 1. El contratante accede al perfil de un trabajador.<br>2. El sistema muestra el listado de reseñas, de solo lectura. |
| **Poscondiciones** | *El contratante visualiza las reseñas sin posibilidad de alterarlas.* |

### 2.4 Requerimiento Funcional 4

| Campo | Descripción |
|------|-------------|
| **ID** | RF-04 |
| **Nombre del requerimiento** | Reporte de reseña falsa o inapropiada |
| **Descripción** | *El sistema debe permitir a un usuario reportar una reseña que considere falsa o inapropiada* |
| **Precondiciones** | *La reseña debe existir y estar visible* |
| **Actor** | *Trabajador o Contratante* |
| **Flujo principal** | 1. El usuario selecciona "Reportar".<br>2. El sistema registra el reporte con estado "Pendiente de revisión". |
| **Poscondiciones** | *El reporte queda registrado y pendiente de moderación.* |

### 2.5 Requerimiento Funcional 5

| Campo | Descripción |
|------|-------------|
| **ID** | RF-05 |
| **Nombre del requerimiento** | Pausado automático por baja reputación |
| **Descripción** | *El sistema debe pausar automáticamente a un trabajador cuyo promedio sea inferior a 3 estrellas en sus últimas 5 reseñas* |
| **Precondiciones** | *El trabajador debe tener al menos 5 reseñas* |
| **Actor** | *Sistema (proceso automático)* |
| **Flujo principal** | 1. El sistema detecta una nueva reseña.<br>2. Calcula el promedio de las últimas 5.<br>3. Si es inferior a 3, cambia el estado a "Pausado". |
| **Poscondiciones** | *El trabajador queda pausado hasta que se defina el proceso de reactivación.* |

### 2.6 Requerimiento Funcional 6

| Campo | Descripción |
|------|-------------|
| **ID** | RF-06 |
| **Nombre del requerimiento** | Penalización por cancelación tardía |
| **Descripción** | *El sistema debe penalizar con 0,5 puntos al trabajador que cancele después del límite de 24 horas* |
| **Precondiciones** | *Debe existir una cancelación tardía notificada por Contratación* |
| **Actor** | *Sistema (proceso automático)* |
| **Flujo principal** | 1. Contratación notifica una cancelación tardía.<br>2. Reputation recibe el evento.<br>3. Descuenta 0,5 puntos del promedio. |
| **Poscondiciones** | *La reputación queda penalizada y reflejada en el perfil.* |

### 2.7 Requerimiento Funcional 7
| Campo | Descripción |
|------|-------------|
| ID | RF-07 |
| Nombre del requerimiento | Asociación de reseña con el servicio realizado |
| Descripción | El sistema debe asociar cada calificación y reseña al servicio que la originó |
| Precondiciones | Debe existir un servicio finalizado y habilitado para calificación |
| Actor | Sistema (proceso automático) |
| Flujo principal | 1. El trabajador o contratante registra una calificación asociada a un servicio finalizado.<br>2. El sistema identifica el servicio correspondiente.<br>3. El sistema registra la reseña vinculándola al identificador del servicio que la originó. |
| Poscondiciones | La reseña queda registrada y asociada de forma trazable al servicio correspondiente. |
