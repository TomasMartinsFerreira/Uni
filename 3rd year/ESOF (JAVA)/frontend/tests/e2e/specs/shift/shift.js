describe('Shift', () => {
  beforeEach(() => {
    cy.deleteAllButArs();
    cy.createDemoEntities();
    cy.createDatabaseInfoForShifts();
  });

  afterEach(() => {
    cy.deleteAllButArs();
  });

  it('create shift', () => {
    const NUMBER = '3';
    const LOCATION = 'super secret location';

    cy.demoMemberLogin()
    // intercept get institutions
    cy.intercept('GET', '/users/*/getInstitution').as('getInstitutions');
    cy.intercept('POST', '/activities/*/shift').as('registerShift');
    // go to create activity form
    cy.get('[data-cy="institution"]').click();

    cy.get('[data-cy="activities"]').click();
    cy.wait('@getInstitutions');

    cy.get('[data-cy="manageShifts"]').click();

    cy.get('[data-cy="newShift"]').click();

     // fill form
    cy.get('[data-cy="LocationInput"]').type(LOCATION);
    cy.get('[data-cy="participantsNumberInput"]').type(NUMBER);

    // select dates
    cy.get('#startTimeInput-input').click();
    cy.get('#startTimeInput-wrapper.date-time-picker')
      .find('.datepicker-day-text')
      .eq(1)
      .click({force: true});
    cy.get('#endDateInput-input').click();
    cy.get('#endDateInput-wrapper.date-time-picker')
      .find('.datepicker-day-text')
      .eq(2)
      .click({force: true});
    cy.get('[data-cy="saveShift"]').click()

    cy.wait('@registerShift');

    // check results
    cy.get('[data-cy="memberShiftsTable"] tbody tr')
      .should('have.length', 1)
      .first()
      .eq(0)
      .children()
      .should('have.length', 4)
    cy.get('[data-cy="memberShiftsTable"] tbody tr')
      .eq(0).children().eq(0).should('contain', LOCATION);
    cy.get('[data-cy="memberShiftsTable"] tbody tr')
      .eq(0).children().eq(1).should('contain', NUMBER);

    cy.logout();
  });

  it('end time before start error', () => {
    const NUMBER = '3';
    const LOCATION = 'super secret location';

    cy.demoMemberLogin()
    // intercept get institutions
    cy.intercept('GET', '/users/*/getInstitution').as('getInstitutions');
    cy.intercept('POST', '/activities/*/shift').as('registerShift');
    // go to create activity form
    cy.get('[data-cy="institution"]').click();

    cy.get('[data-cy="activities"]').click();
    cy.wait('@getInstitutions');

    cy.get('[data-cy="manageShifts"]').click();

    cy.get('[data-cy="newShift"]').click();

     // fill form
    cy.get('[data-cy="LocationInput"]').type(LOCATION);
    cy.get('[data-cy="participantsNumberInput"]').type(NUMBER);

    // select dates
    cy.get('#startTimeInput-input').click();
    cy.get('#startTimeInput-wrapper.date-time-picker')
      .find('.datepicker-day-text')
      .eq(2)
      .click({force: true});
    cy.get('#endDateInput-input').click();
    cy.get('#endDateInput-wrapper.date-time-picker')
      .find('.datepicker-day-text')
      .eq(1)
      .click({force: true});
    cy.get('[data-cy="saveShift"]').click()

    // check error
    cy.wait('@registerShift').then((interception) => {
      expect(interception.response.statusCode).to.eq(400);
      expect(interception.response.body.message)
        .to.eq('Shift start time must be before end time');
    });

    // ensure invalid shift was not added
    cy.get('[data-cy="memberShiftsTable"] tbody tr')
      .should('have.length', 1);

    cy.logout();
  });

  it('shift date outside of activity date', () => {
    const NAME = 'Elderly Assistance';
    const REGION = 'Lisbon';
    const NUMBER = '3';
    const DESCRIPTION = 'Play card games with elderly over 80';
    const LOCATION = 'super secret location';

    cy.demoMemberLogin()
    cy.intercept('POST', '/activities').as('register');
    // intercept get institutions
    cy.intercept('GET', '/users/*/getInstitution').as('getInstitutions');
    cy.intercept('GET', '/themes/availableThemes').as('availableTeams')
    cy.intercept('POST', '/activities/*/shift').as('registerShift');
    // go to create activity form
    cy.get('[data-cy="institution"]').click();

    cy.get('[data-cy="activities"]').click();
    cy.wait('@getInstitutions');

    cy.get('[data-cy="newActivity"]').click();
    cy.wait('@availableTeams');

    // fill form
    cy.get('[data-cy="nameInput"]').type(NAME);
    cy.get('[data-cy="regionInput"]').type(REGION);
    cy.get('[data-cy="participantsNumberInput"]').type(NUMBER);
    cy.get('[data-cy="descriptionInput"]').type(DESCRIPTION);

    // select dates
    cy.get('#applicationDeadlineInput-input').click();
    cy.get('#applicationDeadlineInput-wrapper.date-time-picker')
      .find('.datepicker-day-text')
      .eq(0)
      .click({force: true});
    cy.get('#startingDateInput-input').click();
    cy.get('#startingDateInput-wrapper.date-time-picker')
      .find('.datepicker-day-text')
      .eq(1)
      .click({force: true});
    cy.get('#endingDateInput-input').click();
    cy.get('#endingDateInput-wrapper.date-time-picker')
      .find('.datepicker-day-text')
      .eq(2)
      .click({force: true});

    // save
    cy.get('[data-cy="saveActivity"]').click()
    // check request was done
    cy.wait('@register')

    cy.get('[data-cy="memberActivitiesTable"] tbody tr')
      .eq(0)
      .find('[data-cy="manageShifts"]').click();

    cy.get('[data-cy="newShift"]').click();

     // fill form
    cy.get('[data-cy="LocationInput"]').type(LOCATION);
    cy.get('[data-cy="participantsNumberInput"]').type(NUMBER);

    // select dates
    cy.get('#startTimeInput-input').click();
    cy.get('#startTimeInput-wrapper.date-time-picker')
      .find('.datepicker-day-text')
      .eq(0)
      .click({force: true});
    cy.get('#endDateInput-input').click();
    cy.get('#endDateInput-wrapper.date-time-picker')
      .find('.datepicker-day-text')
      .eq(2)
      .click({force: true});
    cy.get('[data-cy="saveShift"]').click()

    // check error
    cy.wait('@registerShift').then((interception) => {
      expect(interception.response.statusCode).to.eq(400);
      expect(interception.response.body.message)
        .to.eq('Shift dates must be within activity date range');
    });
    
    // ensure invalid shift was not added
    cy.get('[data-cy="memberShiftsTable"] tbody tr')
      .should('have.length', 1);

    // exit error message
    cy.get('body').type('{esc}');

      // select dates
    cy.get('#startTimeInput-input').click();
    cy.get('#startTimeInput-wrapper.date-time-picker')
      .find('.datepicker-day-text')
      .eq(1)
      .click({force: true});
    cy.get('#endDateInput-input').click();
    cy.get('#endDateInput-wrapper.date-time-picker')
      .find('.datepicker-day-text')
      .eq(3)
      .click({force: true});
    cy.get('[data-cy="saveShift"]').click()
    
    cy.wait('@registerShift').then((interception) => {
      expect(interception.response.statusCode).to.eq(400);
      expect(interception.response.body.message)
        .to.eq('Shift dates must be within activity date range');
    });
    
    // ensure invalid shift was not added
    cy.get('[data-cy="memberShiftsTable"] tbody tr')
      .should('have.length', 1);
    // exit error message
    cy.get('body').type('{esc}');

    // select dates
    cy.get('#startTimeInput-input').click();
    cy.get('#startTimeInput-wrapper.date-time-picker')
      .find('.datepicker-day-text')
      .eq(0)
      .click({force: true});
    cy.get('#endDateInput-input').click();
    cy.get('#endDateInput-wrapper.date-time-picker')
      .find('.datepicker-day-text')
      .eq(3)
      .click({force: true});
    cy.get('[data-cy="saveShift"]').click()
    
    cy.wait('@registerShift').then((interception) => {
      expect(interception.response.statusCode).to.eq(400);
      expect(interception.response.body.message)
        .to.eq('Shift dates must be within activity date range');
    });
    
    // ensure invalid shift was not added
    cy.get('[data-cy="memberShiftsTable"] tbody tr')
      .should('have.length', 1);
    // exit error message
    cy.get('body').type('{esc}');


    cy.logout();
  });
  
  it('Invalid Location shift', () => {
    const NUMBER = '3';
    const LOCATION = 'a'.repeat(19);
    const LOCATION2 = 'a'.repeat(201);

    cy.demoMemberLogin()
    // intercept get institutions
    cy.intercept('GET', '/users/*/getInstitution').as('getInstitutions');
    cy.intercept('POST', '/activities/*/shift').as('registerShift');
    // go to create activity form
    cy.get('[data-cy="institution"]').click();

    cy.get('[data-cy="activities"]').click();
    cy.wait('@getInstitutions');

    cy.get('[data-cy="manageShifts"]').click();

    cy.get('[data-cy="newShift"]').click();

     // fill form
    cy.get('[data-cy="LocationInput"]').type(LOCATION);
    cy.get('[data-cy="participantsNumberInput"]').type(NUMBER);

    // select dates
    cy.get('#startTimeInput-input').click();
    cy.get('#startTimeInput-wrapper.date-time-picker')
      .find('.datepicker-day-text')
      .eq(1)
      .click({force: true});
    cy.get('#endDateInput-input').click();
    cy.get('#endDateInput-wrapper.date-time-picker')
      .find('.datepicker-day-text')
      .eq(2)
      .click({force: true});
    cy.get('[data-cy="saveShift"]').should('be.disabled');

    cy.get('[data-cy="LocationInput"]').type(LOCATION2);

    cy.get('[data-cy="saveShift"]').should('be.disabled');

    cy.logout();

  });

  it('Overcapacity shifts in activity', () => {
    const LOCATION = 'super secret location';

    cy.demoMemberLogin()
    // intercept get institutions
    cy.intercept('GET', '/users/*/getInstitution').as('getInstitutions');
    cy.intercept('POST', '/activities/*/shift').as('registerShift');
    // go to create activity form
    cy.get('[data-cy="institution"]').click();

    cy.get('[data-cy="activities"]').click();
    cy.wait('@getInstitutions');

    cy.get('[data-cy="manageShifts"]').click();

    cy.get('[data-cy="newShift"]').click();

     // fill form
    cy.get('[data-cy="LocationInput"]').type(LOCATION);
    cy.get('[data-cy="participantsNumberInput"]').type('6');

    // select dates
    cy.get('#startTimeInput-input').click();
    cy.get('#startTimeInput-wrapper.date-time-picker')
      .find('.datepicker-day-text')
      .eq(1)
      .click({force: true});
    cy.get('#endDateInput-input').click();
    cy.get('#endDateInput-wrapper.date-time-picker')
      .find('.datepicker-day-text')
      .eq(2)
      .click({force: true});
    cy.get('[data-cy="saveShift"]').click()

    cy.wait('@registerShift').then((interception) => {
      expect(interception.response.statusCode).to.eq(400);
      expect(interception.response.body.message)
        .to.eq('Total participants of shifts exceeds activity limit');
    });

    cy.get('body').type('{esc}');

    cy.get('[data-cy="participantsNumberInput"]').type('{backspace}');

    cy.get('[data-cy="participantsNumberInput"]').type('3');

    cy.get('[data-cy="saveShift"]').click()

    cy.get('[data-cy="newShift"]').click();

    cy.wait('@registerShift')

     // fill form
    cy.get('[data-cy="LocationInput"]').type(LOCATION);
    cy.get('[data-cy="participantsNumberInput"]').type('3');

    // select dates
    cy.get('#startTimeInput-input').click();
    cy.get('#startTimeInput-wrapper.date-time-picker')
      .find('.datepicker-day-text')
      .eq(1)
      .click({force: true});
    cy.get('#endDateInput-input').click();
    cy.get('#endDateInput-wrapper.date-time-picker')
      .find('.datepicker-day-text')
      .eq(2)
      .click({force: true});
    cy.get('[data-cy="saveShift"]').click()

    cy.wait('@registerShift').then((interception) => {
      expect(interception.response.statusCode).to.eq(400);
      expect(interception.response.body.message)
        .to.eq('Total participants of shifts exceeds activity limit');
    });

    cy.get('body').type('{esc}');

    cy.get('[data-cy="participantsNumberInput"]').type('{backspace}');

    cy.get('[data-cy="participantsNumberInput"]').type('2');

    cy.get('[data-cy="saveShift"]').click()
    
    cy.wait('@registerShift');

    // check results
    cy.get('[data-cy="memberShiftsTable"] tbody tr')
      .should('have.length', 2)
      .eq(0)
      .children()
      .should('have.length', 4)
    cy.get('[data-cy="memberShiftsTable"] tbody tr')
      .eq(0).children().eq(0).should('contain', LOCATION);
    cy.get('[data-cy="memberShiftsTable"] tbody tr')
      .eq(0).children().eq(1).should('contain', '2');

    cy.get('[data-cy="memberShiftsTable"] tbody tr')
      .should('have.length', 2)
      .eq(1)
      .children()
      .should('have.length', 4)
    cy.get('[data-cy="memberShiftsTable"] tbody tr')
      .eq(1).children().eq(0).should('contain', LOCATION);
    cy.get('[data-cy="memberShiftsTable"] tbody tr')
      .eq(1).children().eq(1).should('contain', '3');

    cy.logout();
  });

  it('create shift for activity not approved', () => {
    const REASON = 'Some reason';
    const JUSTIFICATION = 'It looks mid'

    cy.demoMemberLogin()
    // intercept get institutions
    cy.intercept('GET', '/users/*/getInstitution').as('getInstitutions');
    cy.intercept('GET', '/activities').as('availableActivities');
    // go to create activity form
    cy.get('[data-cy="institution"]').click();

    cy.get('[data-cy="activities"]').click();
    cy.wait('@getInstitutions');

    // suspend activity
    cy.get('[data-cy="suspendButton"]').click();
    cy.get('[data-cy="suspensionReasonInput"]').type(REASON);
    cy.get('[data-cy="suspendActivity"]').click();

    cy.get('[data-cy="manageShifts"]').click();

    cy.get('[data-cy="newShift"]').should('be.disabled');
    cy.logout();

    // log in as admin and approve activity
    cy.demoAdminLogin();
    cy.get('[data-cy="admin"]').click();
    cy.get('[data-cy="adminActivities"]').click();
    cy.wait('@availableActivities');

    cy.get('[data-cy="validateButton"]').click();
    cy.logout();

    // log in as volunteer and report activity
    cy.demoVolunteerLogin();
    cy.get('[data-cy="volunteerActivities"]').click();
    cy.wait('@availableActivities');

    cy.get('[data-cy="reportButton"]').click();
    cy.get('[data-cy="justificationInput"]').type(JUSTIFICATION);
    cy.get('[data-cy="saveReport"]').click();
    cy.logout();

    // log in as member again adn try to create shift
    cy.demoMemberLogin()
    cy.get('[data-cy="institution"]').click();

    cy.get('[data-cy="activities"]').click();
    cy.wait('@getInstitutions');

    cy.get('[data-cy="manageShifts"]').click();

    cy.get('[data-cy="newShift"]').should('be.disabled');
    cy.logout();

  });

});
