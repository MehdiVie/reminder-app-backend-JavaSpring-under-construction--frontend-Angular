package com.example.reminder.service;
import com.example.reminder.dto.EventRequest;
import com.example.reminder.dto.EventResponse;
import com.example.reminder.dto.MoveOccurrenceRequest;
import com.example.reminder.exception.BadRequestException;
import com.example.reminder.exception.ResourceNotFoundException;
import com.example.reminder.model.Event;
import com.example.reminder.model.RecurrenceType;
import com.example.reminder.model.User;
import com.example.reminder.repository.EventRepository;
import com.example.reminder.security.AuthContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
public class EventService {
    private final EventRepository repo;
    private final EmailService emailService;



    // Allowed sort fields (white list)
    private static final Set<String> ALLOWED_SORTS = Set.of("id", "eventDate", "title", "reminderTime");


    public EventService(EventRepository repository, EmailService emailService) {
        this.repo = repository;
        this.emailService = emailService;
    }


    public  Page<Event> getPagedEventsForUser(User user,Integer page, Integer size, String sortBy,
                                      String direction, LocalDate afterDate, String search) {
        // defaults
        int p = (page == null || page < 0) ? 0 : page;
        int s = (size == null || size <= 0 || size > 100) ? 10 : size;

        // safe direction
        Sort.Direction dir;
        try {
            dir = (direction == null) ? Sort.Direction.ASC : Sort.Direction.valueOf(direction.toUpperCase());
        } catch (IllegalArgumentException e) {
            dir = Sort.Direction.ASC;
        }

        //safe sortBy
        String sortProb = (sortBy == null || !ALLOWED_SORTS.contains(sortBy)) ? "id" : sortBy;

        // stable sort
        Sort sort = Sort.by(new Sort.Order(dir, sortProb) , new Sort.Order(dir , "id"));

        Pageable pageable = PageRequest.of(p,s,sort);

        if(afterDate != null && search != null && !search.isEmpty()) {
            String likeSearch = "%" + search.toLowerCase().trim() + "%";
            return repo.findByUserAndAfterDateAndSearch(user, afterDate, likeSearch, pageable);
        } else if (afterDate != null) {
            return repo.findByUserAndAfterDate(user,afterDate, pageable);
        } else if (search != null && !search.isEmpty()){
            String likeSearch = "%" + search.toLowerCase().trim() + "%";

            return repo.findByUserAndSearch(user, likeSearch, pageable);
        }


        return repo.findByUser(user,pageable);


    }

    public  Page<Event> getPagedEventsForAdmin(Integer page, Integer size, String sortBy,
                                              String direction, LocalDate afterDate, String search) {
        // defaults
        int p = (page == null || page < 0) ? 0 : page;
        int s = (size == null || size <= 0 || size > 100) ? 10 : size;

        // safe direction
        Sort.Direction dir;
        try {
            dir = (direction == null) ? Sort.Direction.ASC : Sort.Direction.valueOf(direction.toUpperCase());
        } catch (IllegalArgumentException e) {
            dir = Sort.Direction.ASC;
        }

        //safe sortBy
        String sortProb = (sortBy == null || !ALLOWED_SORTS.contains(sortBy)) ? "id" : sortBy;

        // stable sort
        Sort sort = Sort.by(new Sort.Order(dir, sortProb) , new Sort.Order(dir , "id"));

        Pageable pageable = PageRequest.of(p,s,sort);

        if (afterDate != null && search != null && !search.isEmpty()) {
            String likeSearch = "%" + search.toLowerCase().trim() + "%";
            return repo.findAllEventsAndAfterDateAndSearch(afterDate, likeSearch ,pageable);
        } else if (search != null && !search.isEmpty()) {
            String likeSearch = "%" + search.toLowerCase().trim() + "%";
            return repo.findAllEventsAndSearch(likeSearch ,pageable);
        } else if (afterDate != null) {

            return repo.findAllEventsAndAfterDate(afterDate ,pageable);
        }


        return repo.findAll(pageable);

    }

    public List<Map<String,Object>> getEventsPerDay() {

        LocalDate from = LocalDate.now();
        LocalDate to = LocalDate.now().plusDays(29);

        List<Object[]> raw = repo.eventsPerDaySince(from,to);

        List<Map<String,Object>> result = new ArrayList<>();

        for(Object[] row : raw) {
            LocalDate date = (LocalDate) row[0];
            Long count = (Long) row[1];

            Map<String,Object> map = new HashMap<>();
            map.put("date",date);
            map.put("count",count);
            result.add(map);
        }

        System.out.println(result);

        return result;

    }

    public Event getEventById(User user , Long id) {

        Event event =  repo.findById(id).orElse(null);

        if (event == null) {
            throw new ResourceNotFoundException("Event with ID " + id + " not found.");
        }

        if (!event.getUser().equals(user)) {
            throw new SecurityException("Access denied to this event.");
        }

        return event;
    }

    public List<Event> getAllEventsForCurrentUser(User user) {

        log.info("Current authenticated user: {}", user != null ? user.getEmail() : "null");
        return repo.findByUser(user);

    }


    public Event createEvent(User user,EventRequest eventRequest) {

        if (eventRequest == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        Event createdEvent = new Event();
        createdEvent.setTitle(eventRequest.getTitle());
        createdEvent.setDescription(eventRequest.getDescription());
        createdEvent.setEventDate(eventRequest.getEventDate());
        createdEvent.setReminderTime(eventRequest.getReminderTime());
        createdEvent.setRecurrenceType(eventRequest.getRecurrenceType());
        createdEvent.setRecurrenceInterval(eventRequest.getRecurrenceInterval());
        createdEvent.setRecurrenceEndDate(eventRequest.getRecurrenceEndDate());
        createdEvent.setUser(user);

        return repo.save(createdEvent);
    }

    public Event updateEvent(User user,Long id , EventRequest updatedEvent) {
        Event event = repo.findById(id).orElse(null);

        if (event == null || !event.getUser().equals(user)) return null;

        event.setTitle(updatedEvent.getTitle());
        event.setDescription(updatedEvent.getDescription());
        event.setEventDate(updatedEvent.getEventDate());
        event.setReminderTime(updatedEvent.getReminderTime());
        event.setRecurrenceType(updatedEvent.getRecurrenceType());
        event.setRecurrenceInterval(updatedEvent.getRecurrenceInterval());
        event.setRecurrenceEndDate(updatedEvent.getRecurrenceEndDate());
        event.setReminderSent(false);
        event.setReminderSentTime(null);

        return repo.save(event);
    }

    public void deleteEvent(User user,Long id) {
        Event event = repo.findById(id).orElse(null);

        if (event == null || !event.getUser().equals(user)) {
            throw new SecurityException("Not allowed to delete this event");
        }
        repo.delete(event);
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void checkReminders() {

        LocalDateTime now = LocalDateTime.now().withNano(0);
        /*System.out.println("------------------------------------------------");
        System.out.println("checkReminders() running at: " + now);
        System.out.println("Local time: " + LocalDateTime.now());*/
        List<Event> dueEvents = repo.findPendingReminders(now);
        List<Long> okIds = new ArrayList<>();

        for(Event e : dueEvents) {
            //System.out.println("Scheduler running at: " + now);
            //System.out.println("Its time for event "+e.getTitle()+"("+e.getReminderTime()+")");
            try {
                String html = emailService.buildReminderHtml(e);
                emailService.sendReminderHtml(
                        e.getUser().getEmail(),
                        "Reminder: "+e.getTitle() ,
                        html
                );
                okIds.add(e.getId());

                //handle Recurrence
                createNextOccurenceIfRecurring(e);

            } catch (Exception ex) {
                log.error("Failed to send Email for event {}", e.getId(), ex);
            }
        }

        if (!okIds.isEmpty()) {
            int updated = repo.markRemindersSentByIds(okIds);
            //System.out.println("Proccessed "+updated+" reminders at "+now);
            log.info("Proccessed {} reminders at {} ",updated,now);
        }

    }

    private void createNextOccurenceIfRecurring(Event e) {
        if (e.getRecurrenceType() == null || e.getRecurrenceType() == RecurrenceType.NONE) {
            return;
        }

        int interval =  (e.getRecurrenceInterval() != null || e.getRecurrenceInterval() > 0) ?
                e.getRecurrenceInterval() : 1;

        LocalDate nextDate = e.getEventDate();

        switch (e.getRecurrenceType()) {
            case DAILY -> nextDate = nextDate.plusDays(interval);
            case WEEKLY -> nextDate = nextDate.plusWeeks(interval);
            case MONTHLY -> nextDate = nextDate.plusMonths(interval);
            case YEARLY -> nextDate = nextDate.plusYears(interval);
        }

        if(e.getRecurrenceEndDate() != null && e.getRecurrenceEndDate().isBefore(nextDate)) {
            return;
        }

        Event next = new Event();

        next.setTitle(e.getTitle());
        next.setDescription(e.getDescription());
        next.setUser(e.getUser());
        next.setEventDate(nextDate);

        if (e.getReminderTime() != null ) {
            LocalDateTime nextReminder = e.getReminderTime();
            switch (e.getRecurrenceType()) {
                case DAILY -> nextReminder = nextReminder.plusDays(interval);
                case WEEKLY -> nextReminder = nextReminder.plusWeeks(interval);
                case MONTHLY -> nextReminder = nextReminder.plusMonths(interval);
                case YEARLY -> nextReminder = nextReminder.plusYears(interval);
            }
            next.setReminderTime(nextReminder);
        }

        next.setRecurrenceType(e.getRecurrenceType());
        next.setRecurrenceInterval(interval);
        next.setRecurrenceEndDate(e.getRecurrenceEndDate());

        repo.save(next);
    }

    public List<EventResponse> getCalendarEvents(User user,LocalDate start,LocalDate end) {

        List<EventResponse> result = new ArrayList<>();

        // single events in this range
        List<Event> singles = repo.findSinglesInRange(user,start,end);
        System.out.println("------Singles----"+singles);
        singles.forEach(e-> result.add(EventResponse.fromEntity(e)));

        // exception events in this range
        List<Event> exceptions = repo.findExceptionsInRange(user,start,end);
        System.out.println("------Exceptions----"+exceptions);
        Map <Long , List<Event>> exceptionsByParent =  new HashMap<>();
        for (Event ex :  exceptions) {
            if (ex.getParentEventId() != null) {
                exceptionsByParent
                        .computeIfAbsent(ex.getParentEventId(), k -> new ArrayList<>())
                        .add(ex);
            }
        }

        // recurring masters
        List<Event> masters = repo.findRecurringMasterAffectingRange(user,start,end);
        System.out.println("------Masters----"+masters);
        for (Event master :  masters) {
            List<Event> exForThisMaster = exceptionsByParent.getOrDefault(master.getId(),List.of());
            System.out.println("------INNNN   Masters----"+master.getEventDate());
            result.addAll(
                expandMastersIntoOcurrences(master,start,end,exForThisMaster)
            );
        }

        for(Event ex:exceptions) {
            result.add(EventResponse.fromEntity(ex));
        }
        System.out.println("------All----"+result);

        return result;

    }

    private List<EventResponse> expandMastersIntoOcurrences(
            Event master,
            LocalDate rangeStart, LocalDate rangeEnd,
            List<Event> exceptionForThisMaster
    ) {

        List<EventResponse> list = new ArrayList<>();

        int interval = (master.getRecurrenceInterval() != null || master.getRecurrenceInterval() > 0)
                ? master.getRecurrenceInterval() : 1;

        LocalDate cursor = master.getEventDate();
        System.out.println("------INNNN Z  Expand----"+cursor);
        while (cursor.isBefore(rangeStart)) {
            cursor = addInterval(cursor,master.getRecurrenceType(),interval);

        }

        LocalDate to = (master.getRecurrenceEndDate() != null &&
                master.getRecurrenceEndDate().isBefore(rangeEnd))
                ? master.getRecurrenceEndDate() : rangeEnd;

        Map<LocalDate , Event> exByOriginalDate = new HashMap<>();
        for (Event ex :  exceptionForThisMaster) {
            if (ex.getOriginalDate() != null) {
                exByOriginalDate.put(ex.getOriginalDate(), ex);
            }
        }

        while (!cursor.isAfter(to)) {

            if (exByOriginalDate.containsKey(cursor)) {
                list.add(EventResponse.fromEntity(exByOriginalDate.get(cursor)));
            } else {
                list.add(createOccurrenceFromMaster(master, cursor));
            }

            cursor = addInterval(cursor, master.getRecurrenceType(), interval);
        }

        return list;
    }

    public void moveOccurrence(User user, Long id, MoveOccurrenceRequest req) {
        Event event = repo.findById(id)
                .orElseThrow(()-> new BadRequestException("Event with id " + id + " not found."));

        if (event.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You are not allowed to move this event.");
        }

        String mode = req.getMode() != null ? req.getMode().toUpperCase() : "SINGLE";
        if (!mode.equals("SINGLE")) {
            throw new BadRequestException("Only single mode is allowed for this event.");
        }

        LocalDate originalDate = req.getOriginalDate();
        LocalDate newDate = req.getNewDate();

        if (newDate.isBefore(LocalDate.now())) {
            throw new BadRequestException("New date must be in the future.");
        }

        if (event.isException()) {
            moveExistingException(event, newDate);
            return;
        }

        if (event.getRecurrenceType() == null || event.getRecurrenceType() == RecurrenceType.NONE) {
            throw new  BadRequestException("This event is not recurring.");
        }

        if (originalDate.isBefore(event.getEventDate()) ||
                (event.getRecurrenceEndDate() != null && originalDate.isAfter(event.getRecurrenceEndDate()))
        ) {
            throw new  BadRequestException("Original date is outside the recurrence range.");
        }

        Optional<Event> existingExceptionOpt =
                repo.findByParentEventIdAndOriginalDate(event.getId(), originalDate);

        Event ex;
        if (existingExceptionOpt.isPresent()) {
            ex = existingExceptionOpt.get();
        } else {
            ex = new Event();
            ex.setTitle(event.getTitle());
            ex.setDescription(event.getDescription());
            ex.setParentEventId(event.getId());
            ex.setUser(event.getUser());

            ex.setException(true);
            ex.setOriginalDate(originalDate);

            ex.setRecurrenceType(RecurrenceType.NONE);
            ex.setRecurrenceInterval(null);
            ex.setRecurrenceEndDate(null);
        }

        ex.setEventDate(newDate);

        if (event.getReminderTime() != null) {
            ex.setReminderTime(checkReminderTime(event.getReminderTime(), event.getEventDate()));
        } else {
            ex.setReminderTime(null);
        }

        ex.setReminderSent(false);
        ex.setReminderSentTime(null);

        repo.save(ex);
    }

    private void moveExistingException(Event ex, LocalDate newDate) {
        if (newDate.isBefore(LocalDate.now())) {
            throw new BadRequestException("New date must be in the future.");
        }

        if (ex.getReminderTime() != null) {
            ex.setReminderTime(checkReminderTime(ex.getReminderTime(), ex.getEventDate()));
        }

        ex.setEventDate(newDate);
        ex.setReminderSent(false);
        ex.setReminderSentTime(null);

        repo.save(ex);

    }

    public void moveEventDate(User user,Long  eventId, LocalDate newDate) {

        Event e = repo.findById(eventId)
                .orElseThrow(() -> new BadRequestException("Event not found"));


        if (!e.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You are not allowed to move this event.");
        }


        if (e.getReminderTime() != null) {
            e.setReminderTime(checkReminderTime(e.getReminderTime(), e.getEventDate()));
        }

        e.setEventDate(newDate);
        repo.save(e);
    }


    private LocalDateTime checkReminderTime(LocalDateTime reminderTime,
                                            LocalDate eventDate) {

            LocalDate reminderDateBase =  reminderTime.toLocalDate();
            LocalTime reminderTimeBase = reminderTime.toLocalTime();
            long daysBetween = ChronoUnit.DAYS.between(reminderDateBase , eventDate);
            LocalDateTime newReminderTime = LocalDateTime.of(
                    reminderDateBase.plusDays(daysBetween),
                    reminderTimeBase
            );
            if (newReminderTime.isBefore(LocalDateTime.now())) {
                throw new BadRequestException("New reminderTime would be in the past.");
            }
            return newReminderTime;
    }

    private LocalDate addInterval(LocalDate d, RecurrenceType type, int interval) {
        return switch (type) {
            case DAILY -> d.plusDays(interval);
            case WEEKLY -> d.plusWeeks(interval);
            case MONTHLY -> d.plusMonths(interval);
            case YEARLY -> d.plusYears(interval);
            default -> d;
        };
    }

    private EventResponse createOccurrenceFromMaster(Event master, LocalDate date) {
        EventResponse dto = new EventResponse();

        dto.setId(master.getId());
        dto.setTitle(master.getTitle());
        dto.setDescription(master.getDescription());
        dto.setEventDate(date);

        if (master.getReminderTime() != null) {
            dto.setReminderTime(checkReminderTime(master.getReminderTime(), master.getEventDate()));
        }

        dto.setRecurrenceType(master.getRecurrenceType());
        dto.setRecurrenceInterval(master.getRecurrenceInterval());
        dto.setRecurrenceEndDate(master.getRecurrenceEndDate());

        dto.setParentEventId(master.getId());
        dto.setException(false);
        dto.setOriginalDate(date);

        return dto;
    }




}
