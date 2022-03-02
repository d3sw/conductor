import React, { Component } from 'react';
import moment from 'moment';
import { Link, browserHistory } from 'react-router';
import { Breadcrumb, BreadcrumbItem, Input, Well, Button, Panel, DropdownButton, Grid, ButtonToolbar, MenuItem, Popover, OverlayTrigger, ButtonGroup, Row, Col, Table } from 'react-bootstrap';
import {BootstrapTable, TableHeaderColumn} from 'react-bootstrap-table';
import Typeahead from 'react-bootstrap-typeahead';
import { connect } from 'react-redux';
import { getErrorData } from '../../../actions/WorkflowActions';

const ErrorDashboard = React.createClass({

  getInitialState() {
    return {
      name: '',
      version: '',
      errorData: []
    }
  },

  componentWillMount(){
     const inputData = {
           searchString :this.state.search,
           frmDate :this.state.frmDate,
           toDate: this.state.toDate
          };
        this.props.dispatch(getErrorData(inputData));
  },

  componentWillReceiveProps(nextProps){
    this.state.errorData = nextProps.errorData;
  },

  searchChange(e){
      let val = e.target.value;
      this.setState({ search: val });
    },
    dateChangeFrom(e){
      this.setState({ datefrm: e.target.value });
    },
    dateChangeTo(e){
         this.setState({ dateto: e.target.value });
      },

 rangeChange(range) {
     if (range != null && range.length > 0) {
       let value = range[range.length - 1];
       this.setState({ range: [value] });
       this.state.range = [value];
     } else {
       this.setState({ range: [] });
     }
   },

  clearBtnClick() {
   this.setState({
     datefrm:"",
     dateto: ""
   });
 },

 searchBtnClick() {
        const inputData = {
                 searchString : this.state.search,
                 frmDate : this.state.datefrm,
                 toDate : this.state.dateto,
                 range : this.state.range
                };
              this.props.dispatch(getErrorData(inputData));
      },
  render() {
    var errorData = this.state.errorData;
    var dayErrorCount = 0 ;
    var weekErrorCount = 0 ;
    var monthErrorCount = 0 ;
    var knownErrors = [];
    var unknownErrors = [];
      if (errorData !== undefined && errorData.result !== undefined ) {
          errorData.result.forEach(function (d) {
           var dayFrom = moment().startOf('day').format("YYYY-MM-DD HH:mm:ss");
           var dayEnd = moment().endOf('day').format("YYYY-MM-DD HH:mm:ss");
           const start =  moment(d.startTime).format("YYYY-MM-DD HH:mm:ss");
           const end = moment(d.endTime).format("YYYY-MM-DD HH:mm:ss");
           if (start > dayFrom && end < dayEnd) {
            dayErrorCount = dayErrorCount+d.totalCount;
           }

           var weekFrom = moment().startOf('week').format("YYYY-MM-DD HH:mm:ss");
           var weekEnd = moment().endOf('week').format("YYYY-MM-DD HH:mm:ss");
           if (start > weekFrom && end < weekEnd) {
                       weekErrorCount = weekErrorCount+d.totalCount;
            }

           var monthFrom = moment().startOf('month').format("YYYY-MM-DD HH:mm:ss");
           var monthEnd = moment().endOf('month').format("YYYY-MM-DD HH:mm:ss");
           if (start > monthFrom && end < monthEnd) {
                    monthErrorCount = monthErrorCount+d.totalCount;
           }
           if(d.id === 0)
           {
            unknownErrors.push({
              id: d.id,
              lookup: d.lookup,
              totalCount: d.totalCount
            });
           }
           else
           {
             if(d.isRequiredInReporting == true){
              knownErrors.push({
                        id: d.id,
                        lookup: d.lookup,
                        totalCount: d.totalCount
                       });
                }
           }
          });
        }
    const rangeList = ['All data','This year',
      'Last quarter','This quarter',
      'Last month','This month',
      'Yesterday', 'Today',
      'Last 30 minutes', 'Last 5 minutes'];
    const workflowNames = this.state.workflows?this.state.workflows:[];

    return (
      <div className="ui-content">
        <h1>Workflow Error Dashboard</h1>
         <Panel header="Filter Workflows Errors">
          <Grid fluid={true}>
                     <Row className="show-grid">
                       <Col md={2}>
                         <Typeahead ref="range" onChange={this.rangeChange} options={rangeList} placeholder="Today by default" selected={this.state.range} multiple={true} disabled={this.state.h}/>
                         &nbsp;<i className="fa fa-angle-up fa-1x"></i>&nbsp;&nbsp;<label className="small nobold">Filter by date range</label>
                       </Col>
                       <Col md={4}>
                         <Input type="input" placeholder="Search" groupClassName="" ref="search" value={this.state.search} labelClassName="" onKeyPress={this.keyPress} onChange={this.searchChange}/>
                         &nbsp;<i className="fa fa-angle-up fa-1x"></i>&nbsp;&nbsp;<label className="small nobold">Free Text Query</label>
                         &nbsp;&nbsp;<input type="checkbox" checked={this.state.fullstr} onChange={this.prefChange} ref="fullstr"/><label className="small nobold">&nbsp;Search for entire string</label>
                         </Col>
                       <Col md={5}>
                        <Button bsSize="small" bsStyle="success" onClick={this.clearBtnClick}>&nbsp;&nbsp;Clear date range</Button> &nbsp;&nbsp;
                                                          <Button bsSize="medium" bsStyle="success" onClick={this.searchBtnClick} className="fa fa-search search-label">&nbsp;&nbsp;Search</Button>
                       </Col>
                     </Row>
                      <Row className="show-grid">
                        <Col md={2}>
                           <input  name="datefrm"  type="date" value={this.state.datefrm} className="form-control"  onChange={ this.dateChangeFrom } />
                            &nbsp;<i className="fa fa-angle-up fa-1x"></i>&nbsp;&nbsp;<label className="small nobold">From Date</label>
                         </Col>
                         <Col md={2}>
                            <input  name="dateto"  type="date" value={this.state.dateto} className="form-control"  onChange={ this.dateChangeTo } />
                                &nbsp;<i className="fa fa-angle-up fa-1x"></i>&nbsp;&nbsp;<label className="small nobold">To Date</label>

                           </Col>
                             <Col md={3}>

                              </Col>
                      </Row>
                   </Grid>
         </Panel>
           <Panel header="Error counts">
           <label className="small nobold">Total errors for day : {dayErrorCount}</label><br/>
           <label className="small nobold">Total errors for Week : {weekErrorCount}</label><br/>
           <label className="small nobold">Total errors for month : {monthErrorCount}</label><br/>
           </Panel>
          <Panel header="Unknown Errors">
              <Table striped bordered hover>
                  <thead>
                      <tr>
                      <th>Error Type</th>
                      <th>Total Count</th>
                      </tr>
                    </thead>
                  {unknownErrors !== undefined && unknownErrors.map(item=>(
                 <tbody>
                     <tr>
                        <td> <Link to={'/workflow/errorDashboard/details/'+item.id+'/'+this.state.search+'/'+this.state.datefrm+'/'+this.state.dateto+'/'+this.state.range+'/'+item.lookup }>Unknown Error</Link></td>
                        <td><label className="small nobold">{item.totalCount} </label><br/></td>
                     </tr>
                 </tbody>
                ))}
               </Table>
            </Panel>
           <Panel header="Known Errors">
           <Table striped bordered hover>
              <thead>
                <tr>
                  <th>Error Type</th>
                   <th>Total Count</th>
                </tr>
               </thead>
               {knownErrors !== undefined && knownErrors.map(item=>(
                <tbody>
                 <tr>
                 <td> <Link to={'/workflow/errorDashboard/details/'+item.id+'/'+this.state.search+'/'+this.state.datefrm+'/'+this.state.dateto+'/'+this.state.range+'/'+item.lookup }>{item.lookup}</Link></td>
                 <td><label className="small nobold">{item.totalCount} </label><br/></td>
                 </tr>
                 </tbody>
                ))}
                 </Table>
            </Panel>

      </div>
    );
  }
});
export default connect(state => state.workflow)(ErrorDashboard);
