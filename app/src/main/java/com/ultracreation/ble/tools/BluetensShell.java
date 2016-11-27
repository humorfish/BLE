package com.ultracreation.ble.tools;

import org.w3c.dom.TypeInfo;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import io.reactivex.Observer;
import io.reactivex.subjects.Subject;

import static android.icu.lang.UProperty.LINE_BREAK;
import static java.lang.System.err;

/**
 * Created by Administrator on 2016/11/27.
 */

public class BluetensShell {

     final String SERVICE_UUID = "FFE0";
     final String CHARACTERISTRIC_UUID = "FFE1";
     List<Shell> ShellList = new ArrayList<Shell>();
/* TShell */

     class TShell
    {
        static Get(String DeviceId, int ConnectionTimeout, ClassConstructor<TShell> ShellType )
        {
            for (int i=0; i<ShellList.size(); i++)
            {
                if (shell.DeviceId == DeviceId)
                {
                    shell.ConnectionTimeout = ConnectionTimeout;
                    return shell;
                }
            }

            if (! TypeInfo.Assigned(ShellType))
                ShellType = TShell;

            let RetVal = new ShellType(DeviceId);
            RetVal.ConnectionTimeout = ConnectionTimeout;

            this.ShellList.push(RetVal);
            return RetVal;
        }

        constructor(DeviceId: string)
        {
            this.DeviceId = DeviceId;
            this.RequestList = new Array<TShellRequest>();
        }

        Attach(): void
        {
            this.MakeConnection().catch((err: any) => console.error(err));
        }

        Detach(): void
        {
            BLE.TGatt.Disconnect(this.DeviceId).catch((err: any) => console.error(err));
        }

        MakeConnection(): Promise<BLE.TGapConnection>
        {
            if (TypeInfo.Assigned(this.Connection))
            {
                this.Connection.RefreshTimeout();
                return Promise.resolve(this.Connection);
            }

            if (! TypeInfo.Assigned(this.Connecting))
            {
                this.Connecting = BLE.TGatt.Connect(this.DeviceId, this.ConnectionTimeout, this.NotificationConnectionTimeout)
                        .then((Conn) =>
                        {
                                this.Connection = Conn;
                this.Connecting = null;

                this.Stream = this.Connection.StartNotification(SERVICE_UUID, CHARACTERISTRIC_UUID, TShellStream);
                Conn.subscribe(
                        (next: string) =>
                {
                    this.NotificationConnectionNext(next);
                },
                (err: any) =>
                {
                    this.Stream = null;
                    this.Connection = null;

                    this.NotificatioConnectionError(err);
                },
                () =>
                {
                    this.Stream = null;
                    this.Connection = null;

                    this.NotificationConnectionDisconnect();
                })

                return this.NotificationConnected(Conn)
                        .then(() => Promise.resolve(this.Connection));
                })
                .catch((err: any) =>
                {
                    this.Connecting = null;
                    return Promise.reject(err);
                });
            }

            return this.Connecting;
        }

        Send(CmdOrSomething: string | Array<string> | Uint8Array): void
        {
            this.PromiseSend(CmdOrSomething)
                    .catch((err: any) =>
            {
                console.error(err);
            });
        }

        PromiseSend(CmdOrSomething: string | Array<string> | Uint8Array): Promise<void>
        {
            return this.MakeConnection()
                    .then(() =>
                    {
            if (CmdOrSomething instanceof Uint8Array)
                this.Stream.Write((CmdOrSomething as Uint8Array));
            else if (CmdOrSomething instanceof Object)
        {
            let Str = (CmdOrSomething as Array<string>).join(LINE_BREAK);
            this.Stream.WriteLn(Str);
            console.log(Str);
        }
        else
        {
            this.Stream.WriteLn((CmdOrSomething as string));
            console.log(CmdOrSomething);
        }
            });
        }

        Execute(Cmd: string, IsResponseCallback: (Line: string) => boolean, Timeout: number = 0): Promise<string>
        {
            return new Observer<>((resolve, reject) =>
            {
                String RetVal;
                this.RequestStart(TShellSimpleRequest, Timeout, Cmd, IsResponseCallback).subscribe(
                        (next) =>
                        { RetVal = next; },
                (err) =>
                reject(err),
                        () =>
                resolve(RetVal)
                );
            });
        }

        RequestStart(RequestClass: ClassConstructor<TShellRequest>, Timeout: number = 0, ...args: any[]): TShellRequest
        {
            let RetVal = new RequestClass(this, Timeout);
            this.RequestList.push(RetVal);

            RetVal.Start(...args);
            return RetVal;
        }

        RequestAbort(Request: TShellRequest)
        {
            for (let i = this.RequestList.length - 1; i >= 0; i --)
            {
                if (this.RequestList[i] === Request)
                {
                    this.RequestList.splice(i, 1);

                    // todo: call complete?
                    Request.complete();
                    return;
                }
            }
        }

        /** Notification when Connection Est */
        protected NotificationConnected(Conn: BLE.TGapConnection): Promise<void>
        {
            return Promise.resolve();
        }

        /** Notification when Characteristic Stream readed data */
        protected NotificationConnectionNext(Line: string): void
        {
            console.log(Line);
            this.RequestList.forEach((request) => request.NotificationResponse(Line));
        }

        /** Notification when Connection has any error happened  */
        protected NotificatioConnectionError(err: any): void
        {
            this.RequestList.forEach((request) => request.error(err));
        }

        protected NotificationConnectionTimeout(): Promise<boolean>
        {
            return Promise.resolve(true);
        }

        protected NotificationConnectionDisconnect(): void
        {
            this.RequestList.forEach((request) => request.error(new BLE.BLEDisconnectedError()));
        }

        BLE.TGapConnection Connection;

        protected String DeviceId;
        protected Observer<BLE.TGapConnection> Connecting;
        protected int ConnectionTimeout;

        protected Stream: BLE.TCharacteristicStream;
        protected RequestList: Array<TShellRequest>;

        private static ShellList: ArrayList<TShell> = new ArrayList<TShell>();
    }

/* TShellStream */

     class TShellStream extends BLE.TCharacteristicStream
    {
        TShellStream()
        {
            super(SERVICE_UUID, CHARACTERISTRIC_UUID);

            this.LineBuffer = new TMemoryStream(1024);

            this.LineBreak = TUtf8Encoding.Instance.Encode(LINE_BREAK);
            this.LineBreakMatched = 0;
        }

        private TMemoryStream LineBuffer;
        private Uint8Array LineBreak;
        private int LineBreakMatched;

        /* BLE.TCharacteristicStream */
        /// @override
        int Read(byte[] ByteArray, int  Count)
        {
            throw new ExecutionException("ShellStream.Read is retired, use ReadLn instead");
        }

        /// @override
        void NotificationData(Observer<Object> Listener, ArrayList<Object> buf)
        {
            // do not inherited, the buffer consumed and notified immdiately
            this..Add(buf);
            let byte = new Uint8Array(1);

            while (! this.InBuffer.IsEmpty)
            {
                this.InBuffer.ExtractTo(byte);
                this.LineBuffer.Write(byte);

                if (byte[0] === this.LineBreak[this.LineBreakMatched])
                {
                    this.LineBreakMatched ++;

                    if (this.LineBreakMatched === this.LineBreak.byteLength)
                    {
                        let BytesArray = new Uint8Array(this.LineBuffer.Memory, 0, this.LineBuffer.Position - this.LineBreak.byteLength);
                        Listener.next(TUtf8Encoding.Instance.Decode(BytesArray));

                        this.LineBuffer.Clear();
                        this.LineBreakMatched = 0;
                    }
                }
                else
                this.LineBreakMatched = 0;
            }
        }
    }

/* TShellRequest */
/** generics request support of Shell */

     abstract class TShellRequest extends Subject<Object>
    {
        TShellRequest (TShell Owner, int Timeout)
        {
            super();

            this.Owner = Owner;
            this.TimeoutInterval = Timeout;

            this.RefreshTimeout();
        }

        void abstract Start(Object[] ...args);
        void abstract NotificationResponse(String Line);

        protected RefreshTimeout()
        {
            if (TypeInfo.Assigned(this.TimeoutId))
            {
                clearTimeout(this.TimeoutId);
                this.TimeoutId = null;
            }

            if (this.TimeoutInterval === 0)
                return;

            this.TimeoutId = setTimeout(
                    (Self: TShellRequest) =>
            {
                this.TimeoutId = null;
                Self.error(new BLERequestTimeoutError());
            },
            this.TimeoutInterval, this);
        }

        private Disponse()
        {
            if (TypeInfo.Assigned(this.TimeoutId))
            {
                clearTimeout(this.TimeoutId);
                this.TimeoutId = null;
            }

            this.Owner.RequestAbort(this);
            this.Owner = null;
        }

        /* Subject */
        /// @override
        void complete()
        {
            if (! this.isStopped)
            {
                super.complete();
                this.Disponse();
            }
        }

        /// @override
        void error(Object err)
        {
            if (! this.isStopped)
            {
                super.error(err);
                this.Disponse();
            }
        }

        protected TShell Owner;
        protected int TimeoutId;
        protected int TimeoutInterval;
    }

/* TShellSimpleRequest */
/** the request narrow to 1 ack 1 answer simple request, most cases toPromise */

     class TShellSimpleRequest extends TShellRequest
    {
        /// @override
        Start(String Cmd, boolean IsResponseCallback)
        {
            this.Cmd = Cmd;
            this.IsResponseCallback = IsResponseCallback;

            this.Owner.PromiseSend(this.Cmd)
                    .catch((err)=> this.error(err));
        }

        /// @override
        void NotificationResponse(String Line)
        {
            try
            {
                if (this.IsResponseCallback(Line))
                {
                    this.next(Line);
                    this.complete();
                }
            }
            catch (err)
            {
                this.error(err);
            }
        }

        protected String Cmd;
        protected boolean IsResponseCallback;
    }

}
