# Instrucciones 
Asegurate de tener los siguientes archivos necesarios
-server.c
-cliente.c
-sock-lib.c
-sock-lib.h
-index.html
-pagina.js


TERMINAL 1  (inicio y servidor)

Instalación js
sudo dnf intall nodejs
npm install express

Compila el servidor
gcc server.c -o server

Ejecuta el servidor
./server

agrega evento
add <name> <burstTime> <arrivalTime>
EJ
add evento2 14 0

elimina evento
remove <name>
EJ
remove evento2

anuncia evento (existente) a los clients conectados
trigger <name>
EJ 
trigger evento2

conoce los clients conectados por el nombre del evento
list <name>
EJ 
list evento2

manda a los clients interesados la lista de eventos y algoritmos que se pueden usar
list_algorithm

detiene el servidor y Cierra los sockets de los clients
exit



TERMINAL 2, 3, 4, ...    (Clientes)

Compila el cliente
gcc cliente.c -o cliente2

Ejecuta el cliente
./cliente

solicita los eventos disponibles
ask

se suscribe a un evento
sub <name>
EJ
sub evento2

se desuscribe a un evento
unsub <name>
EJ 
unsun evento2

solicita la lista de eventos a los que esta suscrito
list

solicita lista de algoritmos para organizer los eventos suscritos
ask_algorith

revisa los eventos organizados en un algoritmo mandando valores del 1 al 7, dependiendo del algoritmo deseado
run <numeroDeAlgoritmo>
EJ 
run 1

Termina la aplicacion y se desuscribe de todos los eventos
exit


TERMINAL (js)

Ejecutar JS
node pagina.js


NAVEGADOR
Copia la dirección que se te dió al entrar al servidor

# Como se resolvio cada punto
1 Debe recibir comandos desde la línea de comandos y al mismo tiempo debe ser capaz de escuchar las peticiones (interrupciones) de los clientes.

Para que el servidor pueda esperar comandos desde la terminal y, al mismo tiempo, esperar clientes que se conecten, usamos dos hilos separados: uno para manejar la entrada de comandos desde la terminal y otro para manejar las conexiones de los clientes.

2 Cada interrupción de un cliente será visualizada con un mensaje en la terminal que incluirá el identificador del cliente y el mensaje de la petición.

Esto es más sencillo, simplemente se obtenía el nombre del cliente a partir del socket por el que entraba la información y se agregaba un printf con las especificaciones de las acciones del cliente, como se puede ver en el siguiente ejemplo:

3 Por cada cliente, el servidor deberá permitir seleccionar el algoritmo de scheduling y sobre ese permitir la recepción de comandos, el usuario en cualquier momento podrá cambiar de algoritmo a su gusto.

Esto se logró con una función de run para compilar los procesos a los que el cliente esta suscrito en un algoritmo distinto, desde la terminal del cliente este manda un valor, luego se buscan los eventos de interés del cliente y se obtienen los valores de BT, AT, CT, TT, WT de estos eventos, el siguiente es un ejemplo del FCFS

4 Exit: termina el servidor y deberá publicar este evento a TODOS los clientes.

Cuando se utiliza el comando exit desde el servidor este manda un mensaje a todos los clientes conectados de cerrar el socket, y después cierra el suyo y detiene el programa. Cada cliente cierra su socket y detiene su programa.

5 add event_name: adiciona el evento event_name.

Debido a que trabajamos con procesos simulados, además de agregar el nombre del evento, se agrega también variables del burst time y arrival time para manejarlas después. Esta información se guarda en un arreglo de struct. También se notificó a todos los clientes del evento creado.

6 remove event_name: elimina el evento event_name.

Este caso fue un poco más complicado debido a que debíamos tomar en cuenta factores como las suscripciones y todos los datos de las estructuras de los eventos por lo que cuando un evento se removía se buscaba la información de que clientes estaban suscritos a ese evento y se reemplaza, al igual que la información de los eventos en particular.

7  trigger event_name: publica el evento event_name.

En este caso se usó una función para hacer una llamada a todos los clientes interesados dando información del evento.

8 list event_name: lista todos los clientes suscritos a event_name.

Se busca el índice del evento por nombre y después en la estructura de suscripciones se buscan aquellas con el mismo valor de índice de evento.

9 list algorithm_name: muestra a los clientes los algoritmos disponibles en el event_name.

Se envía a todos los clientes los eventos disponibles que pueden interesarles con la lista de algoritmos que serviran para esos eventos.

10 El cliente debe visualizar en la terminal cada que sea notificado de un evento.

Esta implementación se hizo al mandar mensaje en el punto 5 cada recepción de mensajes en la terminal del cliente se imprime para notificar al cliente.

11 El cliente debe poder seleccionar al algoritmo de planificación que más le interese y ver en todo momento su comportamiento.

Esto se implementó con el comando run del que se platicó en el punto 3

12 sub event_name: se suscribe al evento event_name

Al encontrar el indice del evento por nombre se guardan tanto este indice como el del cliente en una estructura llamada Subs, en donde se guarda la información de todas las suscripciones.

13  unsub event_name: se desuscribe del evento event_name

Una vez encontrados los índices de cliente y evento (como ya se ha hecho en otras funciones del código), se elimina la estructura del arreglo.

14 list: lista todos los eventos a los cuales está suscrito.
Con el identificador del cliente ya encontrado, se itera por el arreglo de la estructura subs para encontrar todos los eventos enlazados al cliente.

15 ask: le pregunta al servidor cuáles eventos hay disponibles.

Esta implementación es muy parecida a la 2.6 la diferencia significativa es que se ordena de otra interfaz y tiene menor información .

16 ask_algorithm: le pregunta al servidor por los algoritmos disponibles a usar en la comunicación.

Este simplemente despliega una lista de los algoritmos disponibles para luego usar la función run con uno de estos IDs.

# Video

https://www.youtube.com/watch?v=Gc-N-muOQOU

# Codigo del proyecto

# server.c
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <fcntl.h>
#include <pthread.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <arpa/inet.h>

// Estructura para representar un evento
struct Event {
    char name[100];
    int burstTime;
    int arrivalTime;
};

// Arreglo global para almacenar eventos
struct Event events[100];
int eventCount = 0;

int clientCount = 0;

int serverSocket;
static pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t cond = PTHREAD_COND_INITIALIZER;

struct client {
    int index;
    int sockID;
    struct sockaddr_in clientAddr;
    int len;
};

struct client Client[1024];

struct subs {
    int eventIndex;
    int clientIndex;
};

struct subs Subs[1024];
int subsCount = 0;
pthread_t thread[1024];
pthread_t commandThread;

const char* getAlgorithmName(int id) {
    switch (id) {
        case 1: return "First Come First Serve (FCFS)";
        case 2: return "First In First Out (FIFO)";
        case 3: return "Round Robin (RR)";
        case 4: return "Shortest Job First (SJF)";
        case 5: return "Shortest Remaining Time (SRT)";
        case 6: return "Highest Response-Ratio Next (HRRN)";
        case 7: return "Multilevel Feedback Queues (MLFQ)";
        default: return "Unknown Algorithm";
    }
}

void runFCFS(int clientIndex, int clientSocket) {
    struct Event subscribedEvents[100];
    int count = 0;
    for (int i = 0; i < subsCount; i++) {
        if (Subs[i].clientIndex == clientIndex) {
            subscribedEvents[count++] = events[Subs[i].eventIndex];
        }
    }
    // Sort events by arrivalTime
    for (int i = 0; i < count - 1; i++) {
        for (int j = 0; j < count - i - 1; j++) {
            if (subscribedEvents[j].arrivalTime > subscribedEvents[j + 1].arrivalTime) {
                struct Event temp = subscribedEvents[j];
                subscribedEvents[j] = subscribedEvents[j + 1];
                subscribedEvents[j + 1] = temp;
            }
        }
    }
    char output[1024] = "Corriendo scheduling algorithm: First Come First Serve (FCFS)\n";
    int l = strlen(output);
    int currentTime = 0;
    for (int i = 0; i < count; i++) {
        int completionTime = currentTime + subscribedEvents[i].burstTime;
        int turnaroundTime = completionTime - subscribedEvents[i].arrivalTime;
        int waitingTime = turnaroundTime - subscribedEvents[i].burstTime;
        l += snprintf(output + l, sizeof(output) - l, 
                      "Nombre: %s, Completion Time: %d, Turnaround Time: %d, Waiting Time: %d\n", 
                      subscribedEvents[i].name, completionTime, turnaroundTime, waitingTime);
        currentTime = completionTime;
    }
    send(clientSocket, output, 1024, 0);
    char output_pagina[4096]; 
    l = snprintf(output_pagina, sizeof(output_pagina), "<h1>Corriendo scheduling algorithm: First Come First Serve (FCFS)</h1><table><tr><th>Nombre</th><th>Completion Time</th><th>Turnaround Time</th><th>Waiting Time</th></tr>");
    currentTime = 0;
    for (int i = 0; i < count; i++) {
        int completionTime = currentTime + subscribedEvents[i].burstTime;
        int turnaroundTime = completionTime - subscribedEvents[i].arrivalTime;
        int waitingTime = turnaroundTime - subscribedEvents[i].burstTime;
        l += snprintf(output_pagina + l, sizeof(output_pagina) - l, 
                    "<tr><td>%s</td><td>%d</td><td>%d</td><td>%d</td></tr>", 
                    subscribedEvents[i].name, completionTime, turnaroundTime, waitingTime);
        currentTime = completionTime;
    }
    // Cerrar la tabla
    strncat(output_pagina + l, "</table>", sizeof(output_pagina) - l);
    // Crear archivo de texto para el cliente
    char filename[256];
    snprintf(filename, sizeof(filename), "output_client_%d.txt", clientIndex);
    FILE *file = fopen(filename, "w");
    if (file == NULL) {
        perror("Error al crear el archivo");
        return;
    }
    fprintf(file, "%s", output_pagina); 
    fclose(file);
}
void runFIFO(int clientIndex, int clientSocket) {
    struct Event subscribedEvents[100];
    int count = 0;
    for (int i = 0; i < subsCount; i++) {
        if (Subs[i].clientIndex == clientIndex) {
            subscribedEvents[count++] = events[Subs[i].eventIndex];
        }
    }
    // Sort events by arrivalTime
    for (int i = 0; i < count - 1; i++) {
        for (int j = 0; j < count - i - 1; j++) {
            if (subscribedEvents[j].arrivalTime > subscribedEvents[j + 1].arrivalTime) {
                struct Event temp = subscribedEvents[j];
                subscribedEvents[j] = subscribedEvents[j + 1];
                subscribedEvents[j + 1] = temp;
            }
        }
    }
    char output[1024] = "Corriendo scheduling algorithm: First In First Out (FIFO)\n";
    int l = strlen(output);
    int currentTime = 0;
    for (int i = 0; i < count; i++) {
        int completionTime = currentTime + subscribedEvents[i].burstTime;
        int turnaroundTime = completionTime - subscribedEvents[i].arrivalTime;
        int waitingTime = turnaroundTime - subscribedEvents[i].burstTime;
        l += snprintf(output + l, sizeof(output) - l,
                      "Nombre: %s, Completion Time: %d, Turnaround Time: %d, Waiting Time: %d\n",
                      subscribedEvents[i].name, completionTime, turnaroundTime, waitingTime);
        currentTime = completionTime;
    }
    send(clientSocket, output, 1024, 0);
    char output_pagina[4096]; 
    l = snprintf(output_pagina, sizeof(output_pagina), "<h1>Corriendo scheduling algorithm: First In First Out (FIFO)</h1><table><tr><th>Nombre</th><th>Completion Time</th><th>Turnaround Time</th><th>Waiting Time</th></tr>");
    currentTime = 0;
    for (int i = 0; i < count; i++) {
        int completionTime = currentTime + subscribedEvents[i].burstTime;
        int turnaroundTime = completionTime - subscribedEvents[i].arrivalTime;
        int waitingTime = turnaroundTime - subscribedEvents[i].burstTime;
        l += snprintf(output_pagina + l, sizeof(output_pagina) - l, 
                    "<tr><td>%s</td><td>%d</td><td>%d</td><td>%d</td></tr>", 
                    subscribedEvents[i].name, completionTime, turnaroundTime, waitingTime);
        currentTime = completionTime;
    }
    // Cerrar la tabla
    strncat(output_pagina + l, "</table>", sizeof(output_pagina) - l);
    // Crear archivo de texto para el cliente
    char filename[256];
    snprintf(filename, sizeof(filename), "output_client_%d.txt", clientIndex);
    FILE *file = fopen(filename, "w");
    if (file == NULL) {
        perror("Error al crear el archivo");
        return;
    }
    fprintf(file, "%s", output_pagina); 
    fclose(file);
}
void runRoundRobin(int clientIndex, int clientSocket) {
    struct Event subscribedEvents[100];
    int count = 0;
    for (int i = 0; i < subsCount; i++) {
        if (Subs[i].clientIndex == clientIndex) {
            subscribedEvents[count++] = events[Subs[i].eventIndex];
        }
    }
    // Initialize variables for Round Robin
    char output[1024] = "Corriendo scheduling algorithm: Round Robin (Quantum = 5)\n";
    int l = strlen(output);
    int quantum = 5;
    int currentTime = 0;
    int remainingBurstTimes[100];
    int completed = 0;
    // Initialize remaining burst times
    for (int i = 0; i < count; i++) {
        remainingBurstTimes[i] = subscribedEvents[i].burstTime;
    }
    // Initialize arrays to keep track of completion, turnaround, and waiting times
    int completionTimes[100] = {0};
    int turnaroundTimes[100] = {0};
    int waitingTimes[100] = {0};
    // Round Robin scheduling loop
    while (completed < count) {
        for (int i = 0; i < count; i++) {
            if (remainingBurstTimes[i] > 0) {
                if (remainingBurstTimes[i] > quantum) {
                    currentTime += quantum;
                    remainingBurstTimes[i] -= quantum;
                } else {
                    currentTime += remainingBurstTimes[i];
                    completionTimes[i] = currentTime;
                    remainingBurstTimes[i] = 0;
                    completed++;
                }
            }
        }
    }
    // Calculate turnaround times and waiting times
    for (int i = 0; i < count; i++) {
        turnaroundTimes[i] = completionTimes[i] - subscribedEvents[i].arrivalTime;
        waitingTimes[i] = turnaroundTimes[i] - subscribedEvents[i].burstTime;
    }
    // Prepare the output
    for (int i = 0; i < count; i++) {
        l += snprintf(output + l, sizeof(output) - l,
                      "Nombre: %s, Completion Time: %d, Turnaround Time: %d, Waiting Time: %d\n",
                      subscribedEvents[i].name, completionTimes[i], turnaroundTimes[i], waitingTimes[i]);
    }
    send(clientSocket, output, 1024, 0);
    char output_pagina[4096]; 
    l = snprintf(output_pagina, sizeof(output_pagina), "<h1>Corriendo scheduling algorithm: Round Robin (Quantum = 5)</h1><table><tr><th>Nombre</th><th>Completion Time</th><th>Turnaround Time</th><th>Waiting Time</th></tr>");
    currentTime = 0;
    for (int i = 0; i < count; i++) {
        int completionTime = currentTime + subscribedEvents[i].burstTime;
        int turnaroundTime = completionTime - subscribedEvents[i].arrivalTime;
        int waitingTime = turnaroundTime - subscribedEvents[i].burstTime;
        l += snprintf(output_pagina + l, sizeof(output_pagina) - l, 
                    "<tr><td>%s</td><td>%d</td><td>%d</td><td>%d</td></tr>", 
                    subscribedEvents[i].name, completionTime, turnaroundTime, waitingTime);
        currentTime = completionTime;
    }
    // Cerrar la tabla
    strncat(output_pagina + l, "</table>", sizeof(output_pagina) - l);
    // Crear archivo de texto para el cliente
    char filename[256];
    snprintf(filename, sizeof(filename), "output_client_%d.txt", clientIndex);
    FILE *file = fopen(filename, "w");
    if (file == NULL) {
        perror("Error al crear el archivo");
        return;
    }
    fprintf(file, "%s", output_pagina); 
    fclose(file); 
}
void runSJF(int clientIndex, int clientSocket) {
    struct Event subscribedEvents[100];
    int count = 0;
    for (int i = 0; i < subsCount; i++) {
        if (Subs[i].clientIndex == clientIndex) {
            subscribedEvents[count++] = events[Subs[i].eventIndex];
        }
    }
    // Sort events by burstTime (Shortest Job First)
    for (int i = 0; i < count - 1; i++) {
        for (int j = 0; j < count - i - 1; j++) {
            if (subscribedEvents[j].burstTime > subscribedEvents[j + 1].burstTime) {
                struct Event temp = subscribedEvents[j];
                subscribedEvents[j] = subscribedEvents[j + 1];
                subscribedEvents[j + 1] = temp;
            }
        }
    }
    char output[1024] = "Corriendo scheduling algorithm: Shortest Job First (SJF)\n";
    int l = strlen(output);
    int currentTime = 0;
    for (int i = 0; i < count; i++) {
        if (currentTime < subscribedEvents[i].arrivalTime) {
            currentTime = subscribedEvents[i].arrivalTime; // Wait for the job to arrive
        }
        int completionTime = currentTime + subscribedEvents[i].burstTime;
        int turnaroundTime = completionTime - subscribedEvents[i].arrivalTime;
        int waitingTime = turnaroundTime - subscribedEvents[i].burstTime;
        l += snprintf(output + l, sizeof(output) - l,
                      "Nombre: %s, Completion Time: %d, Turnaround Time: %d, Waiting Time: %d\n",
                      subscribedEvents[i].name, completionTime, turnaroundTime, waitingTime);
        currentTime = completionTime;
    }
    send(clientSocket, output, 1024, 0);
    char output_pagina[4096]; 
    l = snprintf(output_pagina, sizeof(output_pagina), "<h1>Corriendo scheduling algorithm: Shortest Job First (SJF)</h1><table><tr><th>Nombre</th><th>Completion Time</th><th>Turnaround Time</th><th>Waiting Time</th></tr>");
    currentTime = 0;
    for (int i = 0; i < count; i++) {
        int completionTime = currentTime + subscribedEvents[i].burstTime;
        int turnaroundTime = completionTime - subscribedEvents[i].arrivalTime;
        int waitingTime = turnaroundTime - subscribedEvents[i].burstTime;
        l += snprintf(output_pagina + l, sizeof(output_pagina) - l, 
                    "<tr><td>%s</td><td>%d</td><td>%d</td><td>%d</td></tr>", 
                    subscribedEvents[i].name, completionTime, turnaroundTime, waitingTime);
        currentTime = completionTime;
    }
    // Cerrar la tabla
    strncat(output_pagina + l, "</table>", sizeof(output_pagina) - l);
    // Crear archivo de texto para el cliente
    char filename[256];
    snprintf(filename, sizeof(filename), "output_client_%d.txt", clientIndex);
    FILE *file = fopen(filename, "w");
    if (file == NULL) {
        perror("Error al crear el archivo");
        return;
    }
    fprintf(file, "%s", output_pagina); 
    fclose(file);
}
void runSRTF(int clientIndex, int clientSocket) {
    struct Event subscribedEvents[100];
    int count = 0;
    for (int i = 0; i < subsCount; i++) {
        if (Subs[i].clientIndex == clientIndex) {
            subscribedEvents[count++] = events[Subs[i].eventIndex];
        }
    }
    char output[1024] = "Corriendo scheduling algorithm: Shortest Remaining Time First (SRTF)\n";
    int l = strlen(output);
    int currentTime = 0;
    int completed = 0;
    int remainingBurstTimes[100];
    int arrivalTimes[100];
    int completionTimes[100] = {0};
    int turnaroundTimes[100] = {0};
    int waitingTimes[100] = {0};
    // Initialize remaining burst times and arrival times
    for (int i = 0; i < count; i++) {
        remainingBurstTimes[i] = subscribedEvents[i].burstTime;
        arrivalTimes[i] = subscribedEvents[i].arrivalTime;
    }
    while (completed < count) {
        int minIndex = -1;
        int minBurstTime = 1000;
        // Find the event with the shortest remaining burst time at the current time
        for (int i = 0; i < count; i++) {
            if (arrivalTimes[i] <= currentTime && remainingBurstTimes[i] > 0 && remainingBurstTimes[i] < minBurstTime) {
                minBurstTime = remainingBurstTimes[i];
                minIndex = i;
            }
        }
        if (minIndex == -1) {
            // If no event is found, increment the current time
            currentTime++;
            continue;
        }
        // Execute the event with the shortest remaining burst time
        currentTime++;
        remainingBurstTimes[minIndex]--;
        // If the event is completed, update the completion, turnaround, and waiting times
        if (remainingBurstTimes[minIndex] == 0) {
            completionTimes[minIndex] = currentTime;
            turnaroundTimes[minIndex] = completionTimes[minIndex] - arrivalTimes[minIndex];
            waitingTimes[minIndex] = turnaroundTimes[minIndex] - subscribedEvents[minIndex].burstTime;
            completed++;
        }
    }
    // Prepare the output
    for (int i = 0; i < count; i++) {
        l += snprintf(output + l, sizeof(output) - l,
                      "Nombre: %s, Completion Time: %d, Turnaround Time: %d, Waiting Time: %d\n",
                      subscribedEvents[i].name, completionTimes[i], turnaroundTimes[i], waitingTimes[i]);
    }
    send(clientSocket, output, 1024, 0);
    char output_pagina[4096]; 
    l = snprintf(output_pagina, sizeof(output_pagina), "<h1>Corriendo scheduling algorithm: Shortest Remaining Time First (SRTF)</h1><table><tr><th>Nombre</th><th>Completion Time</th><th>Turnaround Time</th><th>Waiting Time</th></tr>");
    currentTime = 0;
    for (int i = 0; i < count; i++) {
        int completionTime = currentTime + subscribedEvents[i].burstTime;
        int turnaroundTime = completionTime - subscribedEvents[i].arrivalTime;
        int waitingTime = turnaroundTime - subscribedEvents[i].burstTime;
        l += snprintf(output_pagina + l, sizeof(output_pagina) - l, 
                    "<tr><td>%s</td><td>%d</td><td>%d</td><td>%d</td></tr>", 
                    subscribedEvents[i].name, completionTime, turnaroundTime, waitingTime);
        currentTime = completionTime;
    }
    // Cerrar la tabla
    strncat(output_pagina + l, "</table>", sizeof(output_pagina) - l);
    // Crear archivo de texto para el cliente
    char filename[256];
    snprintf(filename, sizeof(filename), "output_client_%d.txt", clientIndex);
    FILE *file = fopen(filename, "w");
    if (file == NULL) {
        perror("Error al crear el archivo");
        return;
    }
    fprintf(file, "%s", output_pagina); 
    fclose(file);
}
void runHRRN(int clientIndex, int clientSocket) {
    struct Event subscribedEvents[100];
    int count = 0;
    for (int i = 0; i < subsCount; i++) {
        if (Subs[i].clientIndex == clientIndex) {
            subscribedEvents[count++] = events[Subs[i].eventIndex];
        }
    }
    char output[1024] = "Corriendo scheduling algorithm: Highest Response-Ratio Next (HRRN)\n";
    int l = strlen(output);
    int currentTime = 0;
    int completed = 0;
    int isCompleted[100] = {0};
    int completionTimes[100] = {0};
    int turnaroundTimes[100] = {0};
    int waitingTimes[100] = {0};
    while (completed < count) {
        int highestResponseRatioIndex = -1;
        float highestResponseRatio = -1.0;
        // Find the event with the highest response ratio
        for (int i = 0; i < count; i++) {
            if (subscribedEvents[i].arrivalTime <= currentTime && !isCompleted[i]) {
                int waitingTime = currentTime - subscribedEvents[i].arrivalTime;
                float responseRatio = (float)(waitingTime + subscribedEvents[i].burstTime) / subscribedEvents[i].burstTime;
                if (responseRatio > highestResponseRatio) {
                    highestResponseRatio = responseRatio;
                    highestResponseRatioIndex = i;
                }
            }
        }
        if (highestResponseRatioIndex == -1) {
            // If no event is ready to be processed, increment the current time
            currentTime++;
            continue;
        }
        // Process the selected event
        int i = highestResponseRatioIndex;
        currentTime += subscribedEvents[i].burstTime;
        completionTimes[i] = currentTime;
        turnaroundTimes[i] = completionTimes[i] - subscribedEvents[i].arrivalTime;
        waitingTimes[i] = turnaroundTimes[i] - subscribedEvents[i].burstTime;
        isCompleted[i] = 1;
        completed++;
    }
    // Prepare the output
    for (int i = 0; i < count; i++) {
        l += snprintf(output + l, sizeof(output) - l,
                      "Nombre: %s, Completion Time: %d, Turnaround Time: %d, Waiting Time: %d\n",
                      subscribedEvents[i].name, completionTimes[i], turnaroundTimes[i], waitingTimes[i]);
    }
    send(clientSocket, output, 1024, 0);
    char output_pagina[4096]; 
    l = snprintf(output_pagina, sizeof(output_pagina), "<h1>Corriendo scheduling algorithm: Highest Response-Ratio Next (HRRN)</h1><table><tr><th>Nombre</th><th>Completion Time</th><th>Turnaround Time</th><th>Waiting Time</th></tr>");
    currentTime = 0;
    for (int i = 0; i < count; i++) {
        int completionTime = currentTime + subscribedEvents[i].burstTime;
        int turnaroundTime = completionTime - subscribedEvents[i].arrivalTime;
        int waitingTime = turnaroundTime - subscribedEvents[i].burstTime;
        l += snprintf(output_pagina + l, sizeof(output_pagina) - l, 
                    "<tr><td>%s</td><td>%d</td><td>%d</td><td>%d</td></tr>", 
                    subscribedEvents[i].name, completionTime, turnaroundTime, waitingTime);
        currentTime = completionTime;
    }
    // Cerrar la tabla
    strncat(output_pagina + l, "</table>", sizeof(output_pagina) - l);
    // Crear archivo de texto para el cliente
    char filename[256];
    snprintf(filename, sizeof(filename), "output_client_%d.txt", clientIndex);
    FILE *file = fopen(filename, "w");
    if (file == NULL) {
        perror("Error al crear el archivo");
        return;
    }
    fprintf(file, "%s", output_pagina); 
    fclose(file);
}
void runMLFQ(int clientIndex, int clientSocket) {
    struct Event subscribedEvents[100];
    int count = 0;
    for (int i = 0; i < subsCount; i++) {
        if (Subs[i].clientIndex == clientIndex) {
            subscribedEvents[count++] = events[Subs[i].eventIndex];
        }
    }
    char output[1024] = "Corriendo scheduling algorithm: Multilevel Feedback Queues (MLFQ)\n";
    int l = strlen(output);
    int currentTime = 0;
    int completed = 0;
    int remainingBurstTimes[100];
    int arrivalTimes[100];
    int completionTimes[100] = {0};
    int turnaroundTimes[100] = {0};
    int waitingTimes[100] = {0};
    int queueLevels[100];
    // Initialize remaining burst times and arrival times
    for (int i = 0; i < count; i++) {
        remainingBurstTimes[i] = subscribedEvents[i].burstTime;
        arrivalTimes[i] = subscribedEvents[i].arrivalTime;
        queueLevels[i] = 0; // All events start at the highest priority queue (level 0)
    }
    while (completed < count) {
        int selectedEvent = -1;
        int quantum = 0;
        // Find the event to process next
        for (int level = 0; level < 3; level++) {
            for (int i = 0; i < count; i++) {
                if (arrivalTimes[i] <= currentTime && remainingBurstTimes[i] > 0 && queueLevels[i] == level) {
                    selectedEvent = i;
                    quantum = (level == 0) ? 5 : (level == 1) ? 10 : remainingBurstTimes[i];
                    break;
                }
            }
            if (selectedEvent != -1) break;
        }
        if (selectedEvent == -1) {
            // If no event is found, increment the current time
            currentTime++;
            continue;
        }
        // Process the selected event
        if (remainingBurstTimes[selectedEvent] > quantum) {
            currentTime += quantum;
            remainingBurstTimes[selectedEvent] -= quantum;
            queueLevels[selectedEvent] = (queueLevels[selectedEvent] < 2) ? queueLevels[selectedEvent] + 1 : 2;
        } else {
            currentTime += remainingBurstTimes[selectedEvent];
            remainingBurstTimes[selectedEvent] = 0;
            completionTimes[selectedEvent] = currentTime;
            turnaroundTimes[selectedEvent] = completionTimes[selectedEvent] - arrivalTimes[selectedEvent];
            waitingTimes[selectedEvent] = turnaroundTimes[selectedEvent] - subscribedEvents[selectedEvent].burstTime;
            completed++;
        }
    }
    // Prepare the output
    for (int i = 0; i < count; i++) {
        l += snprintf(output + l, sizeof(output) - l,
                      "Nombre: %s, Completion Time: %d, Turnaround Time: %d, Waiting Time: %d\n",
                      subscribedEvents[i].name, completionTimes[i], turnaroundTimes[i], waitingTimes[i]);
    }
    send(clientSocket, output, 1024, 0);
    char output_pagina[4096]; 
    l = snprintf(output_pagina, sizeof(output_pagina), "<h1>Corriendo scheduling algorithm: Multilevel Feedback Queues (MLFQ)</h1><table><tr><th>Nombre</th><th>Completion Time</th><th>Turnaround Time</th><th>Waiting Time</th></tr>");
    currentTime = 0;
    for (int i = 0; i < count; i++) {
        int completionTime = currentTime + subscribedEvents[i].burstTime;
        int turnaroundTime = completionTime - subscribedEvents[i].arrivalTime;
        int waitingTime = turnaroundTime - subscribedEvents[i].burstTime;
        l += snprintf(output_pagina + l, sizeof(output_pagina) - l, 
                    "<tr><td>%s</td><td>%d</td><td>%d</td><td>%d</td></tr>", 
                    subscribedEvents[i].name, completionTime, turnaroundTime, waitingTime);
        currentTime = completionTime;
    }
    // Cerrar la tabla
    strncat(output_pagina + l, "</table>", sizeof(output_pagina) - l);
    // Crear archivo de texto para el cliente
    char filename[256];
    snprintf(filename, sizeof(filename), "output_client_%d.txt", clientIndex);
    FILE *file = fopen(filename, "w");
    if (file == NULL) {
        perror("Error al crear el archivo");
        return;
    }
    fprintf(file, "%s", output_pagina); 
    fclose(file);
}

void * doNetworking(void * ClientDetail){
	struct client* clientDetail = (struct client*) ClientDetail;
	int index = clientDetail -> index;
	int clientSocket = clientDetail -> sockID;
	printf("Cliente %d conectado.\n",index + 1);
    // Send welcome message to the client
    char welcomeMessage[1024];
    snprintf(welcomeMessage, sizeof(welcomeMessage), "Tu id de cliente es %d te puedes conectar a la interfaz con el id http://127.0.0.1:8081/output/%d \n", index, index);
    send(clientSocket, welcomeMessage, strlen(welcomeMessage), 0);
	while(1){
		char data[1024];
		int read = recv(clientSocket,data,1024,0);
		data[read] = '\0';
               if(strcmp(data,"ask") == 0){
			int clientIndex = -1;
    			for (int i = 0; i < clientCount; i++) {
        			if (Client[i].sockID == clientSocket) {
            				clientIndex = i;
            				break;
        			}
    			}
    			if (clientIndex != -1) {
        		// Imprimir en la terminal del servidor el identificador del cliente y la acción
        		printf("Cliente %d: Solicitó la lista de eventos.\n", Client[clientIndex].index + 1);
    			}
			send(clientSocket, "Eventos disponibles:", 1024,0);
                        for(int i = 0 ; i < eventCount ; i ++){
				char eventMessage[1024];
                       		sprintf(eventMessage, "Nombre: %s, Burst Time: %d, Arrival Time: %d\n", events[i].name, events[i].burstTime, events[i].arrivalTime);
                        	send(clientSocket, eventMessage,1024,0);
			}
                        continue;
                }
		if (strcmp(data, "sub") == 0) {
            		read = recv(clientSocket, data, 1024, 0);
            		data[read] = '\0';
         		int clientIndex = -1;
                        int eventIndex = -1;
			for (int i = 0; i < clientCount; i++) {
                                if (Client[i].sockID == clientSocket) {
                                        clientIndex = i;
                                        break;
                                }
                        }
			for (int i = 0; i < eventCount; i++) {
        			if (strcmp(events[i].name, data) == 0) {
            				eventIndex = i;
            				break;
        			}
    			}
    			if (eventIndex != -1) {
        			Subs[subsCount].eventIndex = eventIndex;
        			Subs[subsCount].clientIndex = clientIndex;
        			subsCount++;
        			printf("Cliente %d se suscribio al evento: %s\n", clientIndex + 1, data);
    			} else {
        			printf("Evento %s no encontrado.\n", data);
    			}                      
			continue;
        	}
               if(strcmp(data,"list") == 0){
			char output[1024] = "Eventos suscritos:\n";
                        int index = -1;
                        for (int i = 0; i < clientCount; i++) {
                                if (Client[i].sockID == clientSocket) {
                                        index = i;
                                        break;
                                }
                        }
                        if (index != -1) {
                        // Imprimir en la terminal del servidor el identificador del cliente y la acción
                        printf("Cliente %d: Solicitó los eventos a los que esta suscrito.\n", Client[index].index + 1);
                        }
			send(clientSocket, output,1024,0);
                        for (int i = 0; i < subsCount; i++) {
                		if (Subs[i].clientIndex == index) {
            		        char eventMessage[1024];
                                sprintf(eventMessage, "Nombre: %s, Burst Time: %d, Arrival Time: %d\n", events[Subs[i].eventIndex].name, events[Subs[i].eventIndex].burstTime, events[Subs[i].eventIndex].arrivalTime);
                                send(clientSocket, eventMessage,1024,0);
				}
            		}
			continue;
                }
               if(strcmp(data,"unsub") == 0){
                        read = recv(clientSocket, data, 1024, 0);
                        data[read] = '\0';
                        int clientIndex = -1;
                        for (int i = 0; i < clientCount; i++) {
                                if (Client[i].sockID == clientSocket) {
                                        clientIndex = i;
                                        break;
                                }
                        }
    			int eventIndex = -1;
    			for (int i = 0; i < eventCount; i++) {
        			if (strcmp(events[i].name,data) == 0) {
            				eventIndex = i;
            				break;
        			}
    			}
    			if (eventIndex != -1) {
        			for (int i = 0; i < subsCount; i++) {
            				if (Subs[i].eventIndex == eventIndex && Subs[i].clientIndex == clientIndex) {
                				for (int j = i; j < subsCount - 1; j++) {
                    					Subs[j] = Subs[j + 1];
                				}
                				subsCount--;
                				printf("Cliente %d se desuscribio del evento: %s\n", clientIndex + 1, data);
                				break;
            				}
        			}
    			} else {
        			printf("Event %s not found.\n", data);
    			}
                }
		if (strcmp(data, "exit") == 0) {
    			int clientIndex = -1;
    			for (int i = 0; i < clientCount; i++) {
        			if (Client[i].sockID == clientSocket) {
            				clientIndex = i;
            				break;
        			}
    			}
    			if (clientIndex != -1) {
        			int removedCount = 0;
        			for (int i = 0; i < subsCount; i++) {
            				if (Subs[i].clientIndex == clientIndex) {
                				removedCount++;
                // Mover las suscripciones restantes una posición hacia atrás
                				for (int j = i; j < subsCount - 1; j++) {
                    					Subs[j] = Subs[j + 1];
                				}
                				subsCount--;
                				i--; // Decrementar el índice para verificar la suscripción actual nuevamente
            				}
        			}
        			printf("Cliente %d se desuscribio de todos los eventos. Suscripciones eliminadas: %d\n", clientIndex + 1, removedCount);
    			} else {
        			printf("Cliente no encontrado.\n");
    			}
		}
		if (strcmp(data, "run") == 0) {
            		read = recv(clientSocket, data, 1024, 0);
            		data[read] = '\0';
            		int algorithm_id = atoi(data);
            		int clientIndex = -1;
            		for (int i = 0; i < clientCount; i++) {
                		if (Client[i].sockID == clientSocket) {
                    			clientIndex = i;
                   			break;
                		}
            		}
            		if (clientIndex != -1) {
                		printf("Cliente %d: Solicitó ejecutar el algoritmo %s.\n", Client[clientIndex].index + 1, getAlgorithmName(algorithm_id));
		                if (algorithm_id == 1) {
                    			runFCFS(clientIndex, clientSocket);
                		} else if (algorithm_id == 2) {
					runFIFO(clientIndex, clientSocket);
                                } else if (algorithm_id == 3) {
                                        runRoundRobin(clientIndex, clientSocket);
                                } else if (algorithm_id == 4) {
                                        runSJF(clientIndex, clientSocket);
                                } else if (algorithm_id == 5) {
                                        runSRTF(clientIndex, clientSocket);
                                } else if (algorithm_id == 6) {
                                        runHRRN(clientIndex, clientSocket);
                                } else if (algorithm_id == 7) {
                                        runMLFQ(clientIndex, clientSocket);
				} else {
                    			char output[1024];
                    			snprintf(output, sizeof(output), "Algorithm %s not implemented yet.\n", getAlgorithmName(algorithm_id));
                    		send(clientSocket, output, 1024, 0);
				}
            		}
		}
	}
	return NULL;
}

void *commandHandler(void *args) {
    char command[1024]; // Variable para almacenar comandos desde la terminal
    while (1) {
        scanf("%s", command); // Esperar un comando desde la terminal del servidor
        // Aquí puedes agregar lógica para manejar diferentes comandos desde la terminal
	if (strcmp(command, "add") == 0) {
	    char eventName[100];
            int burstTime, arrivalTime;
            scanf("%s %d %d", eventName, &burstTime, &arrivalTime);
            // Guardar el evento en el arreglo global
            strcpy(events[eventCount].name, eventName);
            events[eventCount].burstTime = burstTime;
            events[eventCount].arrivalTime = arrivalTime;
            eventCount++;
            // Formatear el mensaje del evento y guardarlo en la variable eventMessage
            char eventMessage[1024];
	    sprintf(eventMessage, "Evento agregado: %s, Burst Time: %d, Arrival Time: %d\n", eventName, burstTime, arrivalTime);
            printf("%s", eventMessage); // Imprimir el mensaje
            for (int i = 0; i < clientCount; i++) {
                send(Client[i].sockID, eventMessage, 1024, 0);
            }
	}
	if (strcmp(command, "remove") == 0) {
    		int found = 0;
            char eventName[100];
            scanf("%s", eventName);
    		for (int i = 0; i < eventCount; i++) {
        		if (strcmp(events[i].name, eventName) == 0) {
            			found = 1;
            			// Eliminar el evento moviendo los elementos posteriores una posición hacia atrás
            			for (int j = i; j < eventCount - 1; j++) {
                			strcpy(events[j].name, events[j + 1].name);
                			events[j].burstTime = events[j + 1].burstTime;
                			events[j].arrivalTime = events[j + 1].arrivalTime;
            			}
            			eventCount--;                 	
				for (int k = 0; k < subsCount; k++) {
                        		if (Subs[k].eventIndex == i) {
                            			for (int l = k; l < subsCount - 1; l++) {
                                			Subs[l] = Subs[l + 1];
                            			}
                            		subsCount--;
                            		k--; // Revisar nuevamente la posición k tras el corrimiento
                        		} else if (Subs[k].eventIndex > i) {
                            			Subs[k].eventIndex--; // Ajustar los índices de eventos
                       		 	}
                   		}
			 break;
        		}
    		}
    		if (found) {
        		printf("Evento '%s' eliminado correctamente.\n", eventName);
    		} else {
        		printf("El evento '%s' no fue encontrado.\n", eventName);
    		}
	}	
        if (strcmp(command, "trigger") == 0) {
            int found=0;
	    char eventName[100];
            scanf("%s", eventName);
            for (int i = 0; i < eventCount; i++) {
		if (strcmp(events[i].name, eventName) == 0) {
          		found = 1;
            		char eventMessage[1024];
            		sprintf(eventMessage, "Evento que te podria interesar: %s, Burst Time: %d, Arrival Time: %d\n", events[i].name, events[i].burstTime, events[i].arrivalTime);
			for (int i = 0; i < clientCount; i++) {
                		send(Client[i].sockID, eventMessage, 1024, 0);
            		}
                }
	    }
                if (found) {
                        printf("Evento '%s' anunciado correctamente.\n", eventName);
                } else {
                        printf("El evento '%s' no fue encontrado.\n", eventName);
                }
        }
	if (strcmp(command, "list") == 0) {
            char eventName[1024];
            scanf("%s", eventName);
            int eventIndex = -1;
            for (int i = 0; i < eventCount; i++) {
                if (strcmp(events[i].name, eventName) == 0) {
                    eventIndex = i;
                    break;
                }
            }
            if (eventIndex != -1) {
                printf("Clientes suscritos al evento '%s':\n", eventName);
                for (int i = 0; i < subsCount; i++) {
                    if (Subs[i].eventIndex == eventIndex) {
                        int clientIndex = Subs[i].clientIndex;
                        printf("Client %d\n", clientIndex + 1);
                    }
                }
            } else {
                printf("El evento '%s' no fue encontrado.\n", eventName);
            }
        }
	if (strcmp(command, "list_algorithm") == 0) {
		for (int i = 0; i < clientCount; i++) {
			for(int i = 0 ; i < eventCount ; i ++){
                        	char eventMessage[1024];
                        	sprintf(eventMessage, "Nombre: %s, Burst Time: %d, Arrival Time: %d\n", events[i].name, events[i].burstTime, events[i].arrivalTime);
                        	send(Client[i].sockID, eventMessage,1024,0);
                }
                send(Client[i].sockID, "Algoritmos:",1024,0);
                send(Client[i].sockID, "First Come First Serve (FCFS), First In, First Out (FiFo), Round Robin, Shortest Job First, Shortest Remaining Time (SRT), Highest Response-Ratio Next (HRRN) y Multilevel Feedback Queues (MLFQ)",1024,0);
	      	}
		printf("Se envio la lista de los eventos y sus algoritmos a todos los clientes");
	} 
	if (strcmp(command, "exit") == 0) {
            for (int i = 0; i < clientCount; i++) {
                send(Client[i].sockID, "exit", 1024, 0);
            }
	   // Cerrar el socket del servidor
    	   close(serverSocket);
    	   // Terminar el programa
   	   exit(0);
	}
        // Puedes agregar más comandos según tus necesidades
    }
    return NULL;
}
int main() {
    int serverSocket = socket(PF_INET, SOCK_STREAM, 0);
    struct sockaddr_in serverAddr;
    serverAddr.sin_family = AF_INET;
    serverAddr.sin_port = htons(8080);
    serverAddr.sin_addr.s_addr = htons(INADDR_ANY);
    if (bind(serverSocket, (struct sockaddr *)&serverAddr, sizeof(serverAddr)) == -1) return 0;
    if (listen(serverSocket, 1024) == -1) return 0;
    printf("Server started listening on port 8080 ...........\n");
    pthread_create(&commandThread, NULL, commandHandler, NULL); // Hilo para manejar comandos desde la terminal
    while (1) {
        Client[clientCount].sockID = accept(serverSocket, (struct sockaddr *)&Client[clientCount].clientAddr, &Client[clientCount].len);
        Client[clientCount].index = clientCount;
        pthread_create(&thread[clientCount], NULL, doNetworking, (void *)&Client[clientCount]);
        clientCount++;
    }
    for (int i = 0; i < clientCount; i++)
        pthread_join(thread[i], NULL);
    return 0;
}
# client.c
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <fcntl.h>
#include <pthread.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <arpa/inet.h>
int clientSocket;

void salir() {
	printf("Se ha cerrado el socket correctamente");
       	close(clientSocket);
	exit(0);

}
void * doRecieving(void * sockID){
	int clientSocket = *((int *) sockID);
	while(1){
		char data[1024];
		int read = recv(clientSocket,data,1024,0);
		data[read] = '\0';
		printf("%s\n",data);
	      	if (strcmp(data, "exit") == 0) {
            		printf("El servidor ha ordenado cerrar la conexión. Cerrando el cliente...\n");
			salir();
        	}
	}
}
int main(){
  int clientSocket = socket(PF_INET, SOCK_STREAM, 0);
	struct sockaddr_in serverAddr;
	serverAddr.sin_family = AF_INET;
	serverAddr.sin_port = htons(8080);
	serverAddr.sin_addr.s_addr = htonl(INADDR_ANY);
	if(connect(clientSocket, (struct sockaddr*) &serverAddr, sizeof(serverAddr)) == -1) return 0;
	printf("Connection established ............\n");
	pthread_t thread;
	pthread_create(&thread, NULL, doRecieving, (void *) &clientSocket );
	while(1){
		char input[1024];
		scanf("%s",input);
                if(strcmp(input,"list") == 0){
                        send(clientSocket,input,1024,0);
                }
                if(strcmp(input,"ask") == 0){
                        send(clientSocket,input,1024,0);
                }		
		if (strcmp(input, "sub") == 0) {
            		send(clientSocket, input, 1024, 0);
            		scanf("%s", input); // Read the event name
            		send(clientSocket, input, 1024, 0);
            		printf("Suscripcion al evento: %s\n", input);
        	}
                if (strcmp(input, "unsub") == 0) {
                        send(clientSocket, input, 1024, 0);
                        scanf("%s", input); // Read the event name
                        send(clientSocket, input, 1024, 0);
                        printf("Desuscripcion al evento: %s\n", input);
                }
		if (strcmp(input, "ask_algorithm") == 0) {
			printf("Escribe run algorithm_id:\n1 - First Come First Serve (FCFS)\n2 - First In First Out (FiFo)\n3 - Round Robin\n4 - Shortest Job First\n5 - Shortest Remaining Time (SRT)\n6 - Highest Response-Ratio Next (HRRN)\n7 - Multilevel Feedback Queues (MLFQ)\n");
		}
		if (strcmp(input, "run") == 0) {
            		send(clientSocket, input, 1024, 0);
            		scanf("%s", input);
            		send(clientSocket, input, 1024, 0);
        	}
		if (strcmp(input, "exit") == 0) {
			send(clientSocket, input, 1024, 0);
	            	salir();
		}
	}
}
# sock-lib.c
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <fcntl.h>
#include <pthread.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <arpa/inet.h>
int clientSocket;

void salir() {
	printf("Se ha cerrado el socket correctamente");
       	close(clientSocket);
	exit(0);

}
void * doRecieving(void * sockID){
	int clientSocket = *((int *) sockID);
	while(1){
		char data[1024];
		int read = recv(clientSocket,data,1024,0);
		data[read] = '\0';
		printf("%s\n",data);
	      	if (strcmp(data, "exit") == 0) {
            		printf("El servidor ha ordenado cerrar la conexión. Cerrando el cliente...\n");
			salir();
        	}
	}
}
int main(){
	int clientSocket = socket(PF_INET, SOCK_STREAM, 0);
	struct sockaddr_in serverAddr;
	serverAddr.sin_family = AF_INET;
	serverAddr.sin_port = htons(8080);
	serverAddr.sin_addr.s_addr = htonl(INADDR_ANY);
	if(connect(clientSocket, (struct sockaddr*) &serverAddr, sizeof(serverAddr)) == -1) return 0;
	printf("Connection established ............\n");
	pthread_t thread;
	pthread_create(&thread, NULL, doRecieving, (void *) &clientSocket );
	while(1){
		char input[1024];
		scanf("%s",input);
                if(strcmp(input,"list") == 0){
                        send(clientSocket,input,1024,0);
                }
                if(strcmp(input,"ask") == 0){
                        send(clientSocket,input,1024,0);
                }		
		if (strcmp(input, "sub") == 0) {
            		send(clientSocket, input, 1024, 0);
            		scanf("%s", input); // Read the event name
            		send(clientSocket, input, 1024, 0);
            		printf("Suscripcion al evento: %s\n", input);
        	}
                if (strcmp(input, "unsub") == 0) {
                        send(clientSocket, input, 1024, 0);
                        scanf("%s", input); // Read the event name
                        send(clientSocket, input, 1024, 0);
                        printf("Desuscripcion al evento: %s\n", input);
                }
		if (strcmp(input, "ask_algorith") == 0) {
			printf("Escribe run algorithm_id:\n1 - First Come First Serve (FCFS)\n2 - First In First Out (FiFo)\n3 - Round Robin\n4 - Shortest Job First\n5 - Shortest Remaining Time (SRT)\n6 - Highest Response-Ratio Next (HRRN)\n7 - Multilevel Feedback Queues (MLFQ)\n");
		}
		if (strcmp(input, "run") == 0) {
            		send(clientSocket, input, 1024, 0);
            		scanf("%s", input);
            		send(clientSocket, input, 1024, 0);
        	}
		if (strcmp(input, "exit") == 0) {
			send(clientSocket, input, 1024, 0);
	            	salir();
		}
	}


}
# sock-lib.h
/*
 * cambió de nombre de la funcion Open_conection por open_conection
 * cambió de nombre de la funcion  Aceptar_pedidos por aceptar_pedidos
 * en la funcion open_conection se utiliza setsockopt() para eliminar el timeout del puerto al finalizar con Ctrl-C
 * las funciones tienen un parametro 'debug' 0 o 1, para activar mensajes en la consola
 * se puede configurar el numero de puerto y el backlog en la funcion open_conection()
 * se cambio el nombre de la funcion open_conection a abrir_conexion
 * se cambio la funcion conectar para que reciba los parametros hostname y puerto
 *
 */

#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <errno.h>
#include <string.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <sys/wait.h>
#include <arpa/inet.h>
#include <netdb.h>

#define PORT 8000  /* El puerto donde se conectará, servidor */
#define BACKLOG 10 /* Tamaño de la cola de conexiones recibidas */

// api cliente
int conectar(char *hostname, int port, int debug); // funcion que se conecta a un servidor

// api servidor
int abrir_conexion(int port, int backlog, int debug); // función que crea la conexión
int aceptar_pedidos(int, int debug);                  // función que acepta una conexión entrante

# index.html
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Output de Clientes</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            background-color: #f4f4f4;
            margin: 0;
            padding: 20px;
        }
        h1 {
            text-align: center;
            color: #333;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            border: 2px solid #333;
            background-color: #fff;
            margin-bottom: 20px;
        }
        th, td {
            border: 1px solid #ccc;
            padding: 10px;
            text-align: left;
        }
        th {
            background-color: #333;
            color: #fff;
        }
        tr:nth-child(even) {
            background-color: #f2f2f2;
        }
    </style>
</head>
    <body>
        <div id="outputs">
            %%OUTPUT%%
        </div>
    </body>
</html>
# pagina.js
const express = require('express');
const fs = require('fs');
const app = express();

// Ruta para manejar las solicitudes de los archivos de salida de los clientes
app.get('/output/:clientIndex', (req, res) => {
  const clientIndex = req.params.clientIndex;
  const filePath = __dirname + `/output_client_${clientIndex}.txt`;

  // Verificar si el archivo existe
  fs.access(filePath, fs.constants.F_OK, (err) => {
    if (err) {
      // Si hay un error, devolver un mensaje de error al cliente
      res.status(404).send('Archivo de salida no encontrado');
      return;
    }
    // Lectura del archivo HTML
    fs.readFile(__dirname + '/index.html', 'utf8', (err, data) => {
      if (err) {
        // Si hay un error al leer el archivo, devolver un mensaje de error al cliente
        res.status(500).send('Error al cargar el archivo HTML');
        return;
      }
      // Leer el contenido del archivo de salida
      fs.readFile(filePath, 'utf8', (err, outputData) => {
        if (err) {
          // Si hay un error al leer el archivo de salida, devolver un mensaje de error al cliente
          res.status(500).send('Error al leer el archivo de salida');
          return;
        }
        // Reemplazar la etiqueta '%%OUTPUT%%' en la plantilla HTML con los datos del archivo de salida
        let htmlOutput = data.replace('%%OUTPUT%%', outputData);
        // Agregar el número de cliente después de la tabla
        htmlOutput += `<p> ID de cliente: ${parseInt(clientIndex) + 1}</p>`;
        // Enviar el contenido del archivo HTML modificado como respuesta
        res.send(htmlOutput);
      });
    });
  });
});

// Iniciar el servidor en el puerto 8081
app.listen(8081, '127.0.0.1', () => {
  console.log('Servidor escuchando en http://127.0.0.1:8081');
});

