import * as React from "react";
import SnowDataFrame from '../dto/SnowDataFrame';
import SnowDecoder from './SnowDecoder';
import { Stomp, Client } from '@stomp/stompjs';

export interface SnowStreamProps {
    sessionId: string;
    presetName: string;
    width: number;
    height: number;
    fps: number;
}

export default class SnowStream extends React.Component<SnowStreamProps, {}>{
    private decoder: SnowDecoder = new SnowDecoder();
    private stompClient: Client|null = null;
    private isFirstFrame: boolean = true;
    private isActive: boolean = false;

    public startSession() {
        if (this.isActive) {
            return;
        }

        let paramsQuery = "/fps/" + this.props.fps
            + "/width/" + this.props.width
            + "/height/" + this.props.height
            + "/presetName/" + this.props.presetName;

        this.isFirstFrame = true;
        this.isActive = true;

        fetch('http://127.0.0.1:8080/start/' + this.props.sessionId + paramsQuery)
             .then((response) => response.json())
             .then((data) => data.running)
             .then((running) => {
                if (!running) {
                    throw "Server responded that snow session is not running!"
                }
                this.startConsumingWebsocket();
             })
             .catch((err) => {
                console.log(err.message);
             });
    }

    public stopSession() {
        this.stopConsumingWebsocket();
        fetch('http://127.0.0.1:8080/stop/' + this.props.sessionId)
             .then((response) => response.json())
             .then((data) => data.running)
             .then((running) => {
                if (running) {
                    throw "Server responded that snow session is still running!"
                }
                this.isActive = false;
             })
             .catch((err) => {
                console.log(err.message);
             });
    }

    private startConsumingWebsocket() {
        if (this.stompClient) {
            return;
        }
        this.stompClient = new Client({
          brokerURL: 'ws://127.0.0.1:8080/ws/',
          onConnect: (frame) => {
            let userId = frame.headers['user-name'];
            this.stompClient.subscribe('/user/' + userId + '/stream/', this.handleMessage.bind(this));
            this.stompClient.publish({
                destination: '/app/stream/' + this.props.sessionId
            });
          },
        });
        this.stompClient.activate();
    }

    private stopConsumingWebsocket() {
        if (!this.stompClient) {
            return;
        }
        this.stompClient.deactivate();
        this.stompClient = null;
    }

    private handleMessage(message: any) {
        console.log(message);
        return;

        const data = new DataView(message.binaryBody.buffer);
        if (this.isFirstFrame) {
            this.isFirstFrame = false;
            this.handleSnowMetadata(data);
            this.handleSnowBackground(data);
        } else {
            this.handleSnowDataFrame(data);
        }
    }

    private handleSnowMetadata(data : DataView) {
        let metadata = this.decoder.decodeMetadata(data);
        console.log(metadata);
    }

    private handleSnowBackground(data : DataView) {
        let background = this.decoder.decodeBackground(data);
        console.log(background);
    }

    private handleSnowDataFrame(data : DataView) {
        let snowFrame = this.decoder.decodeDataFrame(data);
        console.log("frameNum", snowFrame.frameNum, "chunkSize", snowFrame.chunkSize);
    }

}