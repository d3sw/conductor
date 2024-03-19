import React, {Component} from 'react';
import {connect} from 'react-redux';
import http from '../../core/HttpClient';
import * as authHelper from '../../core/AuthHelper';

class Footer extends Component {

  constructor(props) {
    super(props);
    this.state = {
      sys: {},
      username: null
    };

    http.get('/api/sys/versions').then((data) => {
      this.state.sys = data.sys;
    });
  }

  handleLogoutClick() {
    this.props.dispatch(authHelper.authLogout());
  }

  render() {
    return (
      <div className="Footer navbar-fixed-bottom">
        <div className="row">
          <div className="col-md-4">
            <span className="Footer-text">Server: </span>
            <a href={this.state.sys.server} target="_new" className="small"
               style={{color: 'white'}}>{this.state.sys.server}</a>
          </div>
          <div className="col-md-6">
            <span className="Footer-text">User: </span>
            <span className="small" style={{color: 'white'}}>{this.props.username}</span>
          </div>
          <div className="col-md-2">
            <span className="Footer-text">Conductor Version: </span>
            <span className="small" style={{color: 'white'}}>{this.state.sys.version}</span>&nbsp;&nbsp;
            <br/>
            <span className="Footer-text">Initializer Version: </span>
            <span className="small" style={{color: 'white'}}>{this.state.sys.initializerVersion}</span> &nbsp;&nbsp;
          </div>
        </div>
      </div>
    );
  }
}

export default connect(state => state.workflow)(Footer);