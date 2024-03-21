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
  }

  handleLogoutClick() {
    this.props.dispatch(authHelper.authLogout());
  }

  render() {
    return (
      <div className="Footer navbar-fixed-bottom">
        <div className="row">
        <div className="col-md-4">
        {
          this.props.sys !== null && (
           <div>
             <span className="Footer-text">Server: </span>
              <a href={this.props.sys.server} target="_new" className="small"
                 style={{color: 'white'}}>{this.props.sys.server}</a>
            </div>
          )
        }
         </div>
          <div className="col-md-6">
            <span className="Footer-text">User: </span>
            <span className="small" style={{color: 'white'}}>{this.props.username}</span>
          </div>
          <div className="col-md-2">
              {
                    this.props.sys !== null && (
                      <div>
                        <span className="Footer-text">Conductor Version:</span>
                         <span className="small" style={{color: 'white'}}>{this.props.sys.version}</span>
                        <br/>
                        <span className="Footer-text">Initializer Version: </span>
                          <span className="small" style={{color: 'white'}}>{this.props.sys.initializerVersion}</span>
                      </div>
                    )
                  }
          </div>
        </div>
      </div>
    );
  }
}

export default connect(state => state.workflow)(Footer);