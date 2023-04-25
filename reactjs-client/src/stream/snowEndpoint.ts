import { Client, IMessage } from '@stomp/stompjs';

const snowEndpointUrl="http://127.0.0.1:8080"

export interface SnowAnimationConfiguration {
    presetName: string,
    width: number,
    height: number,
    fps: number,
}

interface EndpointResponse {
    status: boolean,
    sessionId: string,
    running: boolean,
}

export interface DetailsEndpointResponse extends EndpointResponse, SnowAnimationConfiguration {
    exists: boolean,
    message: string,
    streamTextUrl: string,
    streamWebsocketsStompBrokerUrl: string,
    streamWebsocketsUrl: string,
    duration: number,
}

export type StartEndpointResponse = DetailsEndpointResponse;
export type StopEndpointResponse = EndpointResponse;


export function startSnowSession(sessionId: string, config: SnowAnimationConfiguration, controller?: AbortController): Promise<StartEndpointResponse> {
    const url= "/fps/" + config.fps
        + "/width/" + config.width
        + "/height/" + config.height
        + "/presetName/" + config.presetName;

    return askSnowEndpoint(controller, 'start', sessionId, url)
            .then(( response: StartEndpointResponse ) => {
                if (!response.running) {
                    throw Error("Server did not start animation!");
                }
                return response;
            }) as Promise<StartEndpointResponse>;
}

export function fetchSnowDetails(sessionId: string, controller?: AbortController): Promise<DetailsEndpointResponse> {
    if (!sessionId) {
        return Promise.reject(new Error("Session id is missing!"));
    }
    return askSnowEndpoint(controller, 'details', sessionId) as Promise<DetailsEndpointResponse>;
}

export function stopSnowSession(sessionId: string, controller?: AbortController): Promise<StopEndpointResponse>{
    return askSnowEndpoint(controller, 'stop', sessionId)
            .then(( response: StartEndpointResponse ) => {
                if (response.running) {
                    throw Error("Server did not stop animation!");
                }
                return response;
            }) as Promise<StartEndpointResponse>;
}

function askSnowEndpoint(controller: AbortController, action: string, sessionId: string, subUrl: string = ""): Promise<EndpointResponse> {
    const url = `${snowEndpointUrl}/${action}/${sessionId}${subUrl}`;

    return fetch(url, { signal: controller?.signal })
        .then((response) => response.json())
        .then((data) => {
            if (!data) {
                throw Error("Invalid server response!");
            }
            if (!data.status) {
                if (data.message) {
                    throw Error("Server respond with error: " + data.message);
                }
                throw Error("Server respond with error!");
            }
            // console.log(url, data);
            return data;
        })
        .catch((error: Error) => {
            if (error.name === 'AbortError') {
                return;
            }
            console.error(error);
            throw error;
        });
}

export type SnowClientHandler = number;
const stompClients = new Map<SnowClientHandler, Client>();
let stompClientsCounter = 0;

export function startSnowDataStream(startSessionResponse: StartEndpointResponse, handleMessage: (data: DataView) => void): SnowClientHandler {
    const stompClient = new Client({
        brokerURL: startSessionResponse.streamWebsocketsStompBrokerUrl,
        onConnect: (frame) => {
            let userId = frame.headers['user-name'];

            stompClient.subscribe('/user/' + userId + '/stream/',
                (message: IMessage) => handleMessage(new DataView(message.binaryBody.buffer)));

            stompClient.publish({
                destination: startSessionResponse.streamWebsocketsUrl,
            });
        },
    });
    stompClient.activate();

    const handler = ++stompClientsCounter;
    stompClients.set(handler, stompClient);
    return handler;
}

export function stopSnowDataStream(handler: SnowClientHandler): void {
    stompClients.get(handler).deactivate();
    stompClients.delete(handler);
}