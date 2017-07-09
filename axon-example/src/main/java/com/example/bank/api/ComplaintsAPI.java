package com.example.bank.api;

import com.example.bank.commands.FileComplaintCommand;
import com.example.bank.queries.ComplaintQueryObject;
import com.example.bank.queries.ComplaintQueryObjectRepository;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public class ComplaintsAPI {

    @Autowired
    private ComplaintQueryObjectRepository repository;

    @Autowired
    private CommandGateway commandGateway;

    @PostMapping
    public CompletableFuture<String> fineComplaint(@RequestBody Map<String, String> request) {
        String id = UUID.randomUUID().toString(); // Faster then using global sequence, collision-wise
        return commandGateway.send(new FileComplaintCommand(id, request.get("company"), request.get("description")));
    }

    @GetMapping
    public List<ComplaintQueryObject> findAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ComplaintQueryObject findOne(@PathVariable String id) {
        return repository.findOne(id);
    }
}
