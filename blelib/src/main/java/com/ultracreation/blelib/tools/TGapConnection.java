package com.ultracreation.blelib.tools;

import org.reactivestreams.Subscription;
import org.w3c.dom.TypeInfo;

import java.util.stream.Stream;

import static android.R.attr.value;


/**
 * Created by Administrator on 2016/12/16.
 */

public class TGapConnection implements IGapConnection
{
    private String _DeviceId;

    TGapConnection(String DeviceId)
    {
        super();

        this._DeviceId = DeviceId;
    }

    String getDeviceId()
    {
        return _DeviceId;
    }


    Disconnect(): Promise<void>
    {
        return TGatt.Disconnect(this._DeviceId);
    }

    SetTimeout(int Timeout, Callback?: () => Promise<boolean>): void
    {
        this.TimeoutInterval = Timeout;
        this.TimeoutCallback = Callback;

        this.RefreshTimeout();
    }

    /** Refresh Timeout when Actions happens, this will delay Disconnect */
    void RefreshTimeout()
    {
        if (TypeInfo.Assigned(this.TimeoutId))
        {
            clearTimeout(this.TimeoutId);
            this.TimeoutId = null;
        }

        if (this.TimeoutInterval === 0)
            return;

        this.TimeoutId = setTimeout(
                (Self: TGapConnection) =>
        {
            this.TimeoutId = null;

            if (TypeInfo.Assigned(this.TimeoutCallback))
            {
                this.TimeoutCallback()
                        .then(value =>
                        {
                if (value)
                {
                    // kill TimeoutId prevent Timeout Callback call RefreshTimeout()
                    if (TypeInfo.Assigned(this.TimeoutId))
                    {
                        clearTimeout(this.TimeoutId);
                        this.TimeoutId = null;
                    }
                    Self.Disconnect();
                }
                else
                {
                    // callback not specify a new timeout
                    if (! TypeInfo.Assigned(this.TimeoutId))
                        this.RefreshTimeout();
                }
                })
                .catch(err => Self.Disconnect());
            }
            else
                Self.Disconnect();
        }, this.TimeoutInterval, this);
    }

    Read(String Service, String Characteristic): Promise<ArrayBuffer>
    {
        return new Promise<ArrayBuffer>((resolve, reject) =>
        {
            let Self = this;
            (window as any).ble.read(this._DeviceId, Service, Characteristic,
                (buf: any) => resolve(buf),
                (err: any) =>
            {
                Self.NotificationError(err);
                reject((err));
            });

            this.RefreshTimeout();
        })
    }

    Write(String Service, String Characteristic, buf: ArrayBuffer): Promise<void>
    {
        if (buf.byteLength > MTU)
            throw new EInvalidArg('Write exceed the BLE MTU ' + MTU.toString());

        return new Promise<void>((resolve, reject) =>
        {
            let Self = this;
            (window as any).ble.write(this._DeviceId, Service, Characteristic, buf,
                () => resolve(),
                (err: any) =>
            {
                Self.NotificationError(err);
                reject((err));
            });

            this.RefreshTimeout();
        });
    }

    WriteNoResponse(String Service, String Characteristic, buf: ArrayBuffer): void
    {
        if (buf.byteLength > MTU)
            throw new EInvalidArg('Write exceed the BLE MTU ' + MTU.toString());

        // let Self = this;
        (window as any).ble.writeWithoutResponse(this._DeviceId, Service, Characteristic, buf,
            () => {},
            (err: any) => console.log(err));

        this.RefreshTimeout();
    }

    StartNotification(String Service, String Characteristic,
                      CharacteristicStreamType?: ClassConstructor<TCharacteristicStream>): TCharacteristicStream
    {
        for (let Stream of this.CharacteristicStreamList)
        {
            if (Stream.Service === Service && Stream.Characteristic === Characteristic)
                return Stream;
        }

        if (! TypeInfo.Assigned(CharacteristicStreamType))
            CharacteristicStreamType = TCharacteristicStream;

        let RetVal = new CharacteristicStreamType();
        this.CharacteristicStreamList.push(RetVal);
        RetVal.Connection = this;

        let Self = this;
        (window as any).ble.startNotification(this._DeviceId, Service, Characteristic,
            (buf: ArrayBuffer) =>
        {
            Self.RefreshTimeout();
            RetVal.NotificationData(this, buf);
        },
        (err: any) =>
        {
            Self.NotificationError(err);
        });

        return RetVal;
    }

    StopNotification(String Service, String Characteristic): void
    {
        for (let i = 0; i < this.CharacteristicStreamList.length; i ++)
        {
            if (this.CharacteristicStreamList[i].Service === Service &&
                    this.CharacteristicStreamList[i].Characteristic === Characteristic)
            {
                let Stream = this.CharacteristicStreamList.splice(i, 1)[0];
                Stream.Connection = null;
                break;
            }
        }
        (window as any).ble.stopNotification(this._DeviceId, Service, Characteristic);
    }

    /** private */
    NotificationDisconnect(): void
    {
        console.log(this._DeviceId + ' disconnected.');
        this.complete();
    }

    /** private */
    NotificationError(err: any): void
    {
        this.error(err);
        this.Disponse();
    }

    private Disponse()
    {
        console.log('GapConnection disponse');

        if (TypeInfo.Assigned(this.TimeoutId))
        {
            clearTimeout(this.TimeoutId);
            this.TimeoutId = null;
        }

        this.CharacteristicStreamList.forEach((Stream) =>
                {
                        Stream.Connection = null;
        });
    }

    private int TimeoutId;
    private int TimeoutInterval = 0;
    private TimeoutCallback: () => Promise<boolean> = null;

    private CharacteristicStreamList = new Array<TCharacteristicStream>();

    /* Subject */
    /// @override
    subscribe(next?: any, err?: any, complete?: any): Subscription
    {
        if (this.observers.length === 0)
            return super.subscribe(next, err, complete);
        else
            throw new EUsageError('GapConnection can be only subscribe once')
    }

    /// @override
    void complete()
    {
        super.complete();
        this.Disponse();
    }

    /// @override
    void error(Object err)
    {
        super.error(err);
        this.Disponse();
    }
}

/* TCharacteristicStream */

class TCharacteristicStream extends TStream
{
    TCharacteristicStream(String Service, String Characteristic)
    {
        super();

        this.Service = Service;
        this.Characteristic = Characteristic;

        this.InBuffer = new TLoopBuffer(NOTIFICATION_BUFFER);
    }

    void StopNotification()
    {
        if (TypeInfo.Assigned(this.Connection))
            this.Connection.StopNotification(this.Service, this.Characteristic);
    }

    /// @private: call from TGapConnection
    void NotificationData(Listener: Observer<any>, buf: ArrayBuffer)
    {
        // cache the buffer
        this.InBuffer.Push(new Uint8Array(buf));
        // notify Listener
        Listener.next(buf);
    }

    /** TStream */
    /// @override
    int Read(ByteArray: Uint8Array, Count?: number)
    {
        if (! TypeInfo.Assigned(Count) || Count > ByteArray.byteLength)
            Count = ByteArray.byteLength;

        if (Count !== ByteArray.byteLength)
        {
            let view = new Uint8Array(ByteArray.buffer, ByteArray.byteOffset, Count);
            return this.InBuffer.ExtractTo(view);
        }
        else
            return this.InBuffer.ExtractTo(ByteArray);
    }

    /// @override
    int Write(ByteArray: Uint8Array, Count?: number)
    {
        if (! TypeInfo.Assigned(this.Connection))
            return;
        if (! TypeInfo.Assigned(Count) || Count > ByteArray.byteLength)
            Count = ByteArray.byteLength;

        let written = 0;
        while (true)
        {
            if (Count > MTU)
            {
                let Buf = new Uint8Array(MTU);
                let View = new Uint8Array(ByteArray.buffer, ByteArray.byteOffset + written, MTU);
                Buf.set(View);

                this.Connection.WriteNoResponse(this.Service, this.Characteristic, Buf.buffer);
                written += MTU;
                Count -= MTU;
            }
            else
            {
                if (ByteArray.byteOffset !== 0 || ByteArray.byteLength > MTU)
                {
                    let Buf = new Uint8Array(Count);
                    let View = new Uint8Array(ByteArray.buffer, ByteArray.byteOffset + written, Count);
                    Buf.set(View);

                    this.Connection.WriteNoResponse(this.Service, this.Characteristic, Buf.buffer);
                }
                else    // same buffer no need to copy
                    this.Connection.WriteNoResponse(this.Service, this.Characteristic, ByteArray.buffer);

                written += Count;
                Count = 0;
                break;
            }
        }

        return written;
    }

    TGapConnection Connection;
    String Service;
    String Characteristic:;
    protected TLoopBuffer InBuffer;
}