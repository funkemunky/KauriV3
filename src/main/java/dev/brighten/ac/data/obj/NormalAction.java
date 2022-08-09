package dev.brighten.ac.data.obj;

import dev.brighten.ac.handler.keepalive.KeepAlive;
import lombok.AllArgsConstructor;

import java.util.function.Consumer;

@AllArgsConstructor
public  class NormalAction {
        public int stamp;
        public Consumer<KeepAlive> action;
}