import * as React from "react";
import { useContext, createContext, useReducer, useState, useEffect, useCallback, PropsWithChildren, Reducer } from 'react';
import { validateSnowSessionId, validateNumberBetween } from './snowSessionValidator';

export const SnowSessionsContext = createContext([]);
export const SnowSessionsDispatchContext = createContext(null);

export const snowConstraints = {
    defaultSessionId: "session-abc",

    minWidth: 40,
    minHeight: 20,
    defaultWidth: 120,
    defaultHeight: 60,
    maxWidth: 200,
    maxHeight: 150,

    minFps: 1,
    defaultFps: 25,
    maxFps: 66,

    defaultPreset: "massiveSnow",
    presets: {
        classical: "Classical",
        massiveSnow: "Massive Snow",
        calm: "Calm",
        windy: "Windy",
        snowy: "Snowy",
        noSnow: "No snow",
    },
};

export type SessionErrorStatus =
        | "error"
        | "error-cannot-start-new"
        | "error-cannot-start-existing"
        | "error-cannot-stop";

export type SessionStatus =
        | "stopped-not-checked"
        | "stopped-not-found"
        | "stopped-found"
        | "buffering"
        | "playing"
        | "initializing"
        | "checking"
        | SessionErrorStatus;

interface SnowSession {
    sessionId: string,
    presetName: string,
    width: string,
    height: string,
    fps: string,
    animationProgress: number,
    bufferLevel: number,
    status: SessionStatus,
    errorMsg: string|null,

    foundPresetName: string|null,
    foundWidth: number|null,
    foundHeight: number|null,
    foundFps: number|null,
}

interface SessionErrors {
    sessionIdError: string|null,
    widthError: string|null,
    heightError: string|null,
    fpsError: string|null,
}

interface SnowSessionExtraState {
    isSessionExists: boolean|null,
    isSessionIdChanged: boolean,
    cannotStartSession: boolean,
    hasSessionIdError: boolean,
    hasError: boolean,
    isStopped: boolean,
}

interface ValidatedSnowSession extends SnowSession, SessionErrors {
    validatedSessionId: string,
    validatedWidth: number,
    validatedHeight: number,
    validatedFps: number,
}

export type ProcessedSnowSession = ValidatedSnowSession & SnowSessionExtraState;

type DispatchSessionAction =
    | DispatchNewSessionAction
    | DispatchChangeSessionAction
    | DispatchDeleteSessionAction

interface DispatchChangeSessionAction {
    type: string,
    sessionIdx : number,
    changes: object,
}

interface DispatchNewSessionAction {
    type: string,
    newSessionId: string,
}

interface DispatchDeleteSessionAction {
    type: string,
    sessionIdx : number,
}

type DispatchSessionActionWithoutSessionIdx = DistributiveOmit<DispatchSessionAction, "sessionIdx">;

type DistributiveOmit<T, K extends keyof any> = T extends any
  ? Omit<T, K>
  : never;


interface SnowSessionsProviderProps {
    initialSessionId: string,
}

export function SnowSessionsProvider({ initialSessionId, children }: PropsWithChildren<SnowSessionsProviderProps>): JSX.Element {
    const [ sessions, dispatch ] = useReducer<Reducer<ProcessedSnowSession[], DispatchSessionAction>>(snowSessionsReducer, [
        createSession(initialSessionId === ""
            ? snowConstraints.defaultSessionId
            : initialSessionId
        ),
    ]);

    return (
        <SnowSessionsContext.Provider value={sessions} >
            <SnowSessionsDispatchContext.Provider value={dispatch} >
                {children}
            </SnowSessionsDispatchContext.Provider>
        </SnowSessionsContext.Provider>
    );
}

export function useSnowSessions(): ProcessedSnowSession[] {
    return useContext(SnowSessionsContext);
}

export function useSnowSession(sessionIdx : number): ProcessedSnowSession {
    const sessions = useSnowSessions();
    return sessions[sessionIdx];
}

export function useSnowSessionsDispatch(): (action: DispatchSessionAction) => void {
    return useContext(SnowSessionsDispatchContext);
}

export function useSnowSessionDispatch(sessionIdx : number): (action: DispatchSessionActionWithoutSessionIdx) => void {
    const dispatch = useSnowSessionsDispatch();

    return useCallback(
        (action: DispatchSessionActionWithoutSessionIdx) => dispatch({ ...action, sessionIdx: sessionIdx })
    , [ dispatch, sessionIdx ]);
}

export function useDelayedSnowSession(sessionIdx: number, delayMs: number = 70): ProcessedSnowSession {
    const targetSession = useSnowSession(sessionIdx);
    const [ currentSession, setCurrentSession ] = useState(targetSession);

    useEffect(() => {
        const handler = setTimeout(() => {
            setCurrentSession(targetSession);
        }, delayMs);
        return () => {
            clearTimeout(handler);
        };
    }, [ targetSession, delayMs ]);

    return currentSession;
}

function snowSessionsReducer(sessions: ProcessedSnowSession[], action: DispatchSessionAction): ProcessedSnowSession[] {
    switch(action.type) {
       case 'new-session':
            const newSessionAction = action as DispatchNewSessionAction;
            return [
               ...sessions,
               createSession(newSessionAction.newSessionId),
           ];
        case 'session-changed':
            const sessionIdChangeAction = action as DispatchChangeSessionAction;
            const idx = sessionIdChangeAction.sessionIdx;
            const last = sessions[idx];
            const changed = {
                ...last,
                ...sessionIdChangeAction.changes,
            }
            const draft = draftSession(changed, last);

            console.log("changes:", sessionIdChangeAction.changes);

            return [
               ...sessions.slice(0, idx),
                draft,
               ...sessions.slice(idx + 1),
           ];
       case 'delete-session':
            const deleteSessionAction = action as DispatchDeleteSessionAction;
            return [
               ...sessions.slice(0, deleteSessionAction.sessionIdx),
               ...sessions.slice(deleteSessionAction.sessionIdx + 1),
           ];
       case 'accept-or-reject-session-changes':
            return sessions.map(sessionWithAppliedChanges);
    }

    throw Error("Unknown action type: " + action.type)
}

function createSession(initialSessionId: string): ProcessedSnowSession {
    return sessionWithCommittedDraftChanges(false, {
        sessionId : initialSessionId,
        presetName: snowConstraints.defaultPreset,
        width: '' + snowConstraints.defaultWidth,
        height: '' + snowConstraints.defaultHeight,
        fps: '' + snowConstraints.defaultFps,
        animationProgress: 0,
        bufferLevel: 0,
        status: "stopped-not-checked",
        errorMsg: "",

        foundPresetName: null,
        foundWidth: null,
        foundHeight: null,
        foundFps: null,
    });
}

function draftSession(draft: SnowSession, last: ValidatedSnowSession): ProcessedSnowSession {
    const isSessionIdChanged = draft.sessionId !== last.validatedSessionId;
    return postProcessedSession(isSessionIdChanged, {
        ...last,
        ...draft,
        ...( sessionErrors(draft) ),
    });
}

function sessionErrors(draft: SnowSession): SessionErrors {
    return {
       sessionIdError: validateSnowSessionId(draft.sessionId),
       widthError: validateNumberBetween(draft.width, snowConstraints.minWidth, snowConstraints.maxWidth),
       heightError: validateNumberBetween(draft.height, snowConstraints.minHeight, snowConstraints.maxHeight),
       fpsError: validateNumberBetween(draft.fps, snowConstraints.minFps, snowConstraints.maxFps),
   };
}

function sessionWithAppliedChanges(session: ValidatedSnowSession): ProcessedSnowSession {
    const isSessionIdChanged = session.sessionId !== session.validatedSessionId;
    const numOfErrors = Object.values(sessionErrors(session))
         .filter( value => value !== null )
         .length;
    if (numOfErrors === 0) {
        return sessionWithCommittedDraftChanges(isSessionIdChanged, session);
    } else {
        return sessionWithRevertedDraftChanges(session);
    }
}

function sessionWithCommittedDraftChanges(isSessionIdChanged: boolean, draft: SnowSession): ProcessedSnowSession {
    return postProcessedSession(isSessionIdChanged, {
        ...draft,
        ...( sessionErrors(draft) ),
        validatedSessionId: draft.sessionId,
        validatedWidth: parseInt(draft.width),
        validatedHeight: parseInt(draft.height),
        validatedFps: parseInt(draft.fps),
    });
}

function sessionWithRevertedDraftChanges(session: ValidatedSnowSession): ProcessedSnowSession {
    const isSessionIdChanged = session.sessionId !== session.validatedSessionId;

    const revertedSession={
        ...session,
        sessionId: session.validatedSessionId,
        width: '' + session.validatedWidth,
        height: '' + session.validatedHeight,
        fps: '' + session.validatedFps,
    };
    return postProcessedSession(isSessionIdChanged, {
        ...revertedSession,
        ...( sessionErrors(revertedSession) ),
    });
}

function postProcessedSession(isSessionIdChanged: boolean, session: ValidatedSnowSession): ProcessedSnowSession {
    return {
        ...session,
        isSessionIdChanged: isSessionIdChanged,
        hasSessionIdError: session.sessionIdError !== null,
        animationProgress: session.status === 'playing' ? session.animationProgress : 0,
        isSessionExists:
               session.status === 'stopped-found'
            || session.status === 'error-cannot-start-existing',
        cannotStartSession:
               session.status === 'error-cannot-start-new'
            || session.status === 'error-cannot-start-existing',
        hasError:
               session.status === 'error'
            || session.status === 'error-cannot-start-new'
            || session.status === 'error-cannot-start-existing',
        isStopped:
               session.status === 'stopped-not-checked'
            || session.status === 'stopped-not-found'
            || session.status === 'stopped-found'
    };
}
