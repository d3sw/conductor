
const initialState = {
  byStatus: {},
  byId: {},
  storeStateId: 0,
  fetching: false,
  refetch: false,
  terminating: false,
  cancelling: false,
  restarting: false,
  retrying: false,
  cloning: false,
  terminated: {},
  data: [],
  hash: ''
};

export default function workflows(state = initialState, action) {

  let data = state.data;
  switch (action.type) {
    case 'GET_WORKFLOWS':
      return {
        ...state,
        error: false,
        fetching: true
      };
    case 'RECEIVED_WORKFLOWS':
      return {
        ...state,
        data: action.data.result,
        error: false,
        fetching: false,
        refetch: false
      };
    case 'LOCATION_UPDATED':
      return {
        ...state,
        error: false,
        hash: action.location
      };
    case 'GET_WORKFLOW_DETAILS':
      return {
        ...state,
        fetching: true,
        error: false
      };
    case 'RECEIVED_WORKFLOW_DETAILS':
      return {
        ...state,
        data: action.data.result,
        meta: action.data.meta,
        subworkflows: action.data.subworkflows,
        error: false,
        fetching: false,
        refetch: false
      };
    case 'REQUESTED_TERMINATE_WORKFLOW':
      return {
        ...state,
        fetching: true,
        terminating: true
      };
    case 'RECEIVED_TERMINATE_WORKFLOW':
      return {
        ...state,
        error: false,
        data:[],
        fetching: false,
        terminating: false,
        refetch: true
      };
    case 'REQUESTED_CANCEL_WORKFLOW':
      return {
        ...state,
        fetching: true,
        cancelling: true
      };
    case 'RECEIVED_CANCEL_WORKFLOW':
      return {
        ...state,
        error: false,
        data:[],
        fetching: false,
        cancelling: false,
        refetch: true
      };
    case 'REQUESTED_RESTART_WORKFLOW':
      return {
        ...state,
        fetching: true,
        restarting: true
      };
    case 'REQUESTED_CLONE_WORKFLOW':
      return {
        ...state,
        fetching: true,
        restarting: true
      };
    case 'REQUESTED_RETRY_WORKFLOW':
      return {
        ...state,
        fetching: true,
        retrying: true
      };
    case 'REQUESTED_PAUSE_WORKFLOW':
      return {
        ...state,
        fetching: true,
        pausing: true,
        resuming: false

      };
    case 'REQUESTED_RESUME_WORKFLOW':
      return {
        ...state,
        fetching: true,
        resuming: true,
        pausing: false
      };
    case 'RECEIVED_RESTART_WORKFLOW':
      return {
        ...state,
        error: false,
        data:[],
        fetching: false,
        restarting: false,
        refetch: true
      };
     case 'RECEIVED_CLONE_WORKFLOW':
      return {
        ...state,
        error: false,
        cloneWorkflowId: action.data.result,
        fetching: false,
        restarting: false,
        refetch: true
      };
    case 'RECEIVED_RETRY_WORKFLOW':
      return {
        ...state,
        error: false,
        data:[],
        fetching: false,
        restarting: false,
        retrying: false,
        refetch: true
      };
    case 'RECEIVED_PAUSE_WORKFLOW':
      return {
        ...state,
        error: false,
        data:[],
        fetching: false,
        restarting: false,
        retrying: false,
        refetch: true,
        pausing: false,
        resuming: false
      };
    case 'RECEIVED_RESUME_WORKFLOW':
      return {
        ...state,
        error: false,
        data:[],
        fetching: false,
        restarting: false,
        retrying: false,
        refetch: true,
        resuming: false,
        pauing: false
      };
    case 'LIST_WORKFLOWS':
      return {
        ...state,
        error: false,
        fetching: true
      };
    case 'RECEIVED_LIST_WORKFLOWS':
      return {
        ...state,
        workflows: action.workflows.result,
        error: false,
        fetching: false,
        refetch: false
      };
    case 'LIST_EVENT_HANDLERS':
      return {
        ...state,
        error: false,
        fetching: true
      };
    case 'RECEIVED_LIST_EVENT_HANDLERS':
      return {
        ...state,
        events: action.events,
        error: false,
        fetching: false,
        refetch: false
      };
    case 'GET_WORKFLOW_DEF':
      return {
        ...state,
        fetching: true,
        error: false
      };
    case 'RECEIVED_WORKFLOW_DEF':
      return {
        ...state,
        meta: action.workflowMeta.result,
        error: false,
        fetching: false,
        refetch: false
      };
    case 'REQUESTED_UPDATE_WORKFLOW_DEF':
      return {
        ...state,
        updating: true
      };
    case 'RECEIVED_UPDATE_WORKFLOW_DEF':
      return {
        ...state,
        error: false,
        updating: false,
        refetch: true
      };
    case 'GET_TASK_DEFS':
      return {
        ...state,
        error: false,
        fetching: true,
        refetch: false
      };
    case 'RECEIVED_TASK_DEFS':
      return {
        ...state,
        taskDefs: action.taskDefs.result,
        error: false,
        fetching: false,
        refetch: false
      };
    case 'GET_POLL_DATA':
      return {
        ...state,
        error: false,
        fetching: true,
        refetch: false
      };
    case 'RECEIVED_POLL_DATA':
      return {
        ...state,
        queueData: action.queueData.polldata,
        error: false,
        fetching: false,
        refetch: false
      };
    case 'REQUEST_ERROR':
    return {
      ...state,
      error: true,
      exception: action.e,
      fetching: false,
      restarting: false,
      terminating: false,
      cancelling: false,
      retrying: false,
      pausing: false,
      resuming: false
    };
    case 'GET_TASK_LOGS':
      return {
        ...state,
        fetching: true,
        error: false
      };
    case 'RECEIVED_GET_TASK_LOGS':
      return {
        ...state,
        logs: action.logs,
        error: false,
        fetching: false,
        refetch: false
      };
     case 'REQUESTED_ERROR_DATA':
            return {
              ...state,
              fetching: true,
              error: false
            };
     case 'RECEIVED_ERROR_DATA':
            return {
              ...state,
              errorData: action.errorData,
              error: false,
              fetching: false,
              refetch: false
            };
    case 'REQUESTED_ERROR_DAY_DATA':
                 return {
                   ...state,
                   fetching: true,
                   error: false
       };
      case 'RECEIVED_ERROR_DAY_DATA':
                 return {
                   ...state,
                   errorDataDay: action.errorDataDay,
                   error: false,
                   fetching: false,
                   refetch: false
        };
      case 'REQUESTED_ERROR_WEEK_DATA':
                      return {
                        ...state,
                        fetching: true,
                        error: false
            };
       case 'RECEIVED_ERROR_WEEK_DATA':
                      return {
                        ...state,
                        errorDataWeek: action.errorDataWeek,
                        error: false,
                        fetching: false,
                        refetch: false
       };
        case 'REQUESTED_ERROR_DATA_MONTH':
                  return {
                    ...state,
                    fetching: true,
                    error: false
                  };
           case 'RECEIVED_ERROR_DATA_MONTH':
                  return {
                    ...state,
                    errorDataMonth: action.errorDataMonth,
                    error: false,
                    fetching: false,
                    refetch: false
                  };
     case 'REQUESTED_ERRORLIST_DATA':
                return {
                  ...state,
                  fetching: true,
                  error: false
                };
    case 'RECEIVED_ERRORLIST_DATA':
                return {
                  ...state,
                  errorData: action.errorData,
                  error: false,
                  fetching: false,
                  refetch: false
                };
    default:
      return state;
    };
}
