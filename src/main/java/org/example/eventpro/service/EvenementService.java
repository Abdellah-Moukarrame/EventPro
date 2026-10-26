package org.example.eventpro.service;

import org.example.eventpro.dto.request.EventCreateRequest;
import org.example.eventpro.dto.request.EventUpdateRequest;
import org.example.eventpro.dto.response.EventResponseDTO;

import java.util.List;

public interface EvenementService {

    EventResponseDTO create(EventCreateRequest request);

    EventResponseDTO findById(Long id);

    List<EventResponseDTO> findAll();

    EventResponseDTO update(Long id, EventUpdateRequest request);

    void delete(Long id);
}