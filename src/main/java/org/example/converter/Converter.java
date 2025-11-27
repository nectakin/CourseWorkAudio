package org.example.converter;

import org.example.audiotrack.Audiotrack;

public interface Converter<T> {
    T convertTo(Audiotrack audiotrack);
}